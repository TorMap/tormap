package org.tormap.cli

import java.net.InetAddress
import java.net.UnknownHostException
import java.net.spi.InetAddressResolver
import java.net.spi.InetAddressResolver.LookupPolicy
import java.net.spi.InetAddressResolverProvider
import java.util.stream.Stream

/**
 * Keeps host names away from the local resolver while a request goes through a SOCKS proxy.
 *
 * HttpURLConnection resolves the target host itself before it connects through a SOCKS proxy and then sends only the
 * IP address to the proxy, so a DNS request for the API host would leave outside Tor. While [LocalDns.refuse] runs,
 * every lookup on that thread fails, so the JDK keeps the address unresolved and the proxy gets the host name
 * (SOCKS5 domain name address type) and resolves it.
 *
 * Registered in META-INF/services, which native-image includes from the classpath as well.
 */
class LocalDnsResolverProvider : InetAddressResolverProvider() {
    override fun get(configuration: Configuration): InetAddressResolver = LocalDnsResolver(configuration.builtinResolver())

    override fun name() = "tormap-cli"
}

internal class LocalDnsResolver(private val builtin: InetAddressResolver) : InetAddressResolver {
    override fun lookupByName(host: String, lookupPolicy: LookupPolicy): Stream<InetAddress> {
        if (LocalDns.isRefused()) throw UnknownHostException("$host is resolved by the proxy")
        return builtin.lookupByName(host, lookupPolicy)
    }

    override fun lookupByAddress(address: ByteArray): String {
        if (LocalDns.isRefused()) throw UnknownHostException("No reverse lookups while using a proxy")
        return builtin.lookupByAddress(address)
    }
}

internal object LocalDns {
    private val refused = ThreadLocal.withInitial { false }

    fun isRefused(): Boolean = refused.get()

    /** Runs [block] with all name lookups of this thread refused */
    fun <T> refuse(block: () -> T): T {
        val previous = refused.get()
        refused.set(true)
        try {
            return block()
        } finally {
            refused.set(previous)
        }
    }
}
