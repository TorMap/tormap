package org.tormap.service

import org.springframework.beans.factory.DisposableBean
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.tormap.config.CacheConfig
import org.tormap.util.logger
import org.xbill.DNS.AAAARecord
import org.xbill.DNS.ARecord
import org.xbill.DNS.ExtendedResolver
import org.xbill.DNS.Lookup
import org.xbill.DNS.Name
import org.xbill.DNS.PTRRecord
import org.xbill.DNS.Record
import org.xbill.DNS.ReverseMap
import org.xbill.DNS.Type
import java.net.Inet6Address
import java.net.InetAddress
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

data class ReverseDnsLookupResult(
    val verifiedHostNames: List<String> = emptyList(),
    val unverifiedHostNames: List<String> = emptyList(),
)

interface ReverseDnsResolver {
    fun lookupPtrRecords(ipAddress: String): List<String>
    fun lookupAddresses(hostName: String): List<String>
}

/**
 * Thrown when all reverse DNS lookup slots are busy. Exceptions are not cached, so a later request retries the lookup.
 */
class ReverseDnsLookupCapacityException : RuntimeException("All reverse DNS lookup slots are busy")

@Component
class DNSJavaReverseDnsResolver : ReverseDnsResolver {
    companion object {
        // Per query, across all configured name servers
        private val QUERY_TIMEOUT: Duration = Duration.ofSeconds(1)
    }

    private val logger = logger()
    private val dnsResolver = ExtendedResolver().apply { timeout = QUERY_TIMEOUT }

    override fun lookupPtrRecords(ipAddress: String): List<String> {
        return try {
            lookup(ReverseMap.fromAddress(ipAddress), Type.PTR)
                .filterIsInstance<PTRRecord>()
                .map { it.target.toString().trimEnd('.') }
        } catch (e: Exception) {
            logger.debug("PTR record lookup for IP {} failed! {}: {}", ipAddress, e.javaClass, e.message)
            emptyList()
        }
    }

    override fun lookupAddresses(hostName: String): List<String> {
        return try {
            // Absolute name, so the search path of the host is not applied
            val name = Name.fromString(hostName, Name.root)
            listOf(Type.A, Type.AAAA)
                .flatMap { lookup(name, it) }
                .mapNotNull {
                    when (it) {
                        is ARecord -> it.address
                        is AAAARecord -> it.address
                        else -> null
                    }
                }
                .filter { it.isPubliclyRoutable() }
                .map { it.hostAddress }
        } catch (e: Exception) {
            logger.debug("Address lookup for hostname {} failed! {}: {}", hostName, e.javaClass, e.message)
            emptyList()
        }
    }

    private fun lookup(name: Name, type: Int): List<Record> =
        Lookup(name, type).apply { setResolver(dnsResolver) }.run()?.toList() ?: emptyList()

    private fun InetAddress.isULA(): Boolean =
        this is Inet6Address &&
            (address[0].toInt() and 0xFE) == 0xFC  // fc00::/7

    private fun InetAddress.isPubliclyRoutable(): Boolean =
        !isLoopbackAddress &&
            !isLinkLocalAddress &&
            !isSiteLocalAddress &&   // reliable for IPv4
            !isULA() &&              // fills the IPv6 gap isSiteLocalAddress misses
            !isMulticastAddress &&
            !isAnyLocalAddress &&
            address.first() != 0.toByte() &&
            !(address.size == 4 &&
                address[0] == 100.toByte() &&
                (address[1].toInt() and 0xFF) in 64..127)
}

@Service
class ReverseDnsLookupService(
    private val reverseDnsResolver: ReverseDnsResolver,
    private val lookupTimeout: Duration = Duration.ofSeconds(2),
    maxConcurrentLookups: Int = 8,
) : DisposableBean {
    companion object {
        private const val MAX_PTR_HOSTNAMES_TO_VERIFY = 10
    }

    private val logger = logger()

    // Without a queue, so a lookup either starts right away or is rejected
    private val lookupExecutor = ThreadPoolExecutor(
        maxConcurrentLookups,
        maxConcurrentLookups,
        0L,
        TimeUnit.MILLISECONDS,
        SynchronousQueue(),
        Thread.ofPlatform().name("reverse-dns-", 0).daemon().factory(),
    )

    /**
     * Looks up the PTR names of [ipAddress] and verifies them with forward lookups, within [lookupTimeout].
     * Host names that are not verified before the deadline are returned as unverified.
     *
     * @throws ReverseDnsLookupCapacityException if all lookup slots are busy
     */
    @Cacheable(CacheConfig.REVERSE_DNS_LOOKUPS, key = "#ipAddress", sync = true)
    fun lookupHostNames(ipAddress: String): ReverseDnsLookupResult {
        val deadline = System.nanoTime() + lookupTimeout.toNanos()
        val progress = LookupProgress()
        val task = try {
            lookupExecutor.submit { lookupHostNames(ipAddress, deadline, progress) }
        } catch (_: RejectedExecutionException) {
            throw ReverseDnsLookupCapacityException()
        }
        try {
            task.get(lookupTimeout.toNanos(), TimeUnit.NANOSECONDS)
        } catch (_: TimeoutException) {
            task.cancel(true)
            logger.debug("Reverse DNS lookup for IP {} timed out after {}", ipAddress, lookupTimeout)
        } catch (e: ExecutionException) {
            logger.debug("Reverse DNS lookup for IP {} failed! {}: {}", ipAddress, e.cause?.javaClass, e.cause?.message)
        } catch (_: InterruptedException) {
            task.cancel(true)
            Thread.currentThread().interrupt()
        }
        return progress.toResult()
    }

    override fun destroy() {
        lookupExecutor.shutdownNow()
    }

    private fun lookupHostNames(ipAddress: String, deadline: Long, progress: LookupProgress) {
        progress.hostNames = reverseDnsResolver.lookupPtrRecords(ipAddress)
            .map { it.trim().trimEnd('.') }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_PTR_HOSTNAMES_TO_VERIFY)

        for (hostName in progress.hostNames) {
            if (Thread.currentThread().isInterrupted || System.nanoTime() - deadline >= 0) return
            if (reverseDnsResolver.lookupAddresses(hostName).any { it == ipAddress }) {
                progress.verifiedHostNames += hostName
            }
        }
    }

    /** Written by the lookup thread, read by the request thread, also after a timeout. */
    private class LookupProgress {
        @Volatile
        var hostNames: List<String> = emptyList()
        val verifiedHostNames: MutableSet<String> = ConcurrentHashMap.newKeySet()

        fun toResult(): ReverseDnsLookupResult {
            val (verified, unverified) = hostNames.partition { it in verifiedHostNames }
            return ReverseDnsLookupResult(verifiedHostNames = verified, unverifiedHostNames = unverified)
        }
    }
}
