package org.tormap.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.tormap.config.CacheConfig
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Hashtable
import javax.naming.Context
import javax.naming.NamingException
import javax.naming.directory.Attributes
import javax.naming.directory.InitialDirContext

data class ReverseDnsLookupResult(
    val verifiedHostNames: List<String> = emptyList(),
    val unverifiedHostNames: List<String> = emptyList(),
)

interface ReverseDnsResolver {
    fun lookupPtrRecords(ipAddress: String): List<String>
    fun lookupAddresses(hostName: String): List<String>
}

@Component
class JndiReverseDnsResolver : ReverseDnsResolver {
    override fun lookupPtrRecords(ipAddress: String): List<String> {
        val environment = Hashtable<String, String>()
        environment[Context.INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.dns.DnsContextFactory"
        var context: InitialDirContext? = null
        return try {
            context = InitialDirContext(environment)
            val attributes = context.getAttributes(ipAddress.toReverseLookupDomain(), arrayOf("PTR"))
            attributes.getAttributeValues("PTR").map { it.trimEnd('.') }
        } catch (_: NamingException) {
            emptyList()
        } finally {
            context?.close()
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

private fun String.toReverseLookupDomain(): String = split('.').reversed().joinToString(".") + ".in-addr.arpa"

private fun Attributes.getAttributeValues(attributeName: String): List<String> {
    val attribute = get(attributeName) ?: return emptyList()
    return (0 until attribute.size()).map { attribute.get(it).toString() }
}
