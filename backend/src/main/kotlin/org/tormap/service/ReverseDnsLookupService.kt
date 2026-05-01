package org.tormap.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.tormap.config.CacheConfig
import org.xbill.DNS.Lookup
import org.xbill.DNS.PTRRecord
import org.xbill.DNS.ReverseMap
import org.xbill.DNS.Type
import java.net.InetAddress
import java.net.UnknownHostException

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
    override fun lookupPtrRecords(ipAddress: String): List<String> {
        return try {
            val name = ReverseMap.fromAddress(ipAddress)
            val records = Lookup(name, Type.PTR).run() ?: return emptyList()
            records.filterIsInstance<PTRRecord>().map { it.target.toString() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun lookupAddresses(hostName: String): List<String> = try {
        InetAddress.getAllByName(hostName).map { it.hostAddress }
    } catch (_: UnknownHostException) {
        emptyList()
    }
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
