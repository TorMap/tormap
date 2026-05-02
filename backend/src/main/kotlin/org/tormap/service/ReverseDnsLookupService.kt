package org.tormap.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.tormap.config.CacheConfig
import org.tormap.util.logger
import org.xbill.DNS.Lookup
import org.xbill.DNS.PTRRecord
import org.xbill.DNS.ReverseMap
import org.xbill.DNS.Type
import java.net.Inet6Address
import java.net.InetAddress

data class ReverseDnsLookupResult(
    val verifiedHostNames: List<String> = emptyList(),
    val unverifiedHostNames: List<String> = emptyList(),
)

interface ReverseDnsResolver {
    fun lookupPtrRecords(ipAddress: String): List<String>
    fun lookupAddresses(hostName: String): List<String>
}

@Component
class DNSJavaReverseDnsResolver : ReverseDnsResolver {
    private val logger = logger()

    override fun lookupPtrRecords(ipAddress: String): List<String> {
        return try {
            val records = Lookup(ReverseMap.fromAddress(ipAddress), Type.PTR).run()
                ?: return emptyList()
            records.filterIsInstance<PTRRecord>().map { it.target.toString().trimEnd('.') }
        } catch (e: Exception) {
            logger.debug("PTR record lookup for IP $ipAddress failed! ${e.javaClass}: ${e.message}")
            emptyList()
        }
    }

    override fun lookupAddresses(hostName: String): List<String> {
        return try {
            InetAddress.getAllByName(hostName)
                .filter { it.isPubliclyRoutable() }
                .map { it.hostAddress }
        } catch (e: Exception) {
            logger.debug("Address lookup for hostname $hostName failed! ${e.javaClass}: ${e.message}")
            emptyList()
        }
    }

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
) {
    @Cacheable(CacheConfig.REVERSE_DNS_LOOKUPS, key = "#ipAddress")
    fun lookupHostNames(ipAddress: String): ReverseDnsLookupResult {
        val hostNames = reverseDnsResolver.lookupPtrRecords(ipAddress)
            .map { it.trim().trimEnd('.') }
            .filter { it.isNotBlank() }
            .distinct()

        if (hostNames.isEmpty()) {
            return ReverseDnsLookupResult()
        }

        val (verifiedHostNames, unverifiedHostNames) = hostNames.partition { hostName ->
            reverseDnsResolver.lookupAddresses(hostName).any { it == ipAddress }
        }

        return ReverseDnsLookupResult(
            verifiedHostNames = verifiedHostNames,
            unverifiedHostNames = unverifiedHostNames,
        )
    }
}
