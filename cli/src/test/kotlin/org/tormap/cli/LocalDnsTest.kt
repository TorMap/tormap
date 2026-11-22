package org.tormap.cli

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.DataInputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.UnknownHostException
import java.net.spi.InetAddressResolver
import java.net.spi.InetAddressResolver.LookupPolicy
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

class LocalDnsTest : StringSpec({
    val builtin = object : InetAddressResolver {
        override fun lookupByName(host: String, lookupPolicy: LookupPolicy): Stream<InetAddress> =
            Stream.of(InetAddress.getByAddress(host, byteArrayOf(10, 9, 9, 9)))

        override fun lookupByAddress(address: ByteArray) = "resolved.example"
    }
    val policy = LookupPolicy.of(LookupPolicy.IPV4)

    "lookups are refused only inside refuse" {
        val resolver = LocalDnsResolver(builtin)
        resolver.lookupByName("api.tormap.org", policy).count() shouldBe 1
        LocalDns.refuse {
            shouldThrow<UnknownHostException> { resolver.lookupByName("api.tormap.org", policy) }
            shouldThrow<UnknownHostException> { resolver.lookupByAddress(byteArrayOf(10, 9, 9, 9)) }
        }
        resolver.lookupByName("api.tormap.org", policy).count() shouldBe 1
        resolver.lookupByAddress(byteArrayOf(10, 9, 9, 9)) shouldBe "resolved.example"
    }

    "the resolver is installed" {
        LocalDns.refuse {
            shouldThrow<UnknownHostException> { InetAddress.getByName("api.tormap.org") }
        }
    }

    "a SOCKS proxy gets the API host name, not an address resolved locally" {
        // nosemgrep: kotlin.lang.security.unencrypted-socket.unencrypted-socket -- a fake SOCKS proxy on loopback, SOCKS itself is unencrypted
        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
            // A SOCKS5 proxy that records the requested address and then refuses the connection
            val requested = CompletableFuture.supplyAsync {
                server.accept().use { socket ->
                    val input = DataInputStream(socket.getInputStream())
                    val output = socket.getOutputStream()
                    input.readNBytes(2).also { input.readNBytes(it[1].toInt()) }
                    output.write(byteArrayOf(5, 0))
                    input.readNBytes(3)
                    val address = when (val type = input.read()) {
                        1 -> "IPv4 " + InetAddress.getByAddress(input.readNBytes(4)).hostAddress
                        3 -> "domain " + String(input.readNBytes(input.read()))
                        4 -> "IPv6 " + InetAddress.getByAddress(input.readNBytes(16)).hostAddress
                        else -> "address type $type"
                    }
                    output.write(byteArrayOf(5, 2, 0, 1, 0, 0, 0, 0, 0, 0))
                    address
                }
            }
            val api = ApiClient(
                "https://api.tormap.org",
                ApiClient.parseProxy("socks5h://localhost:${(server.localSocketAddress as InetSocketAddress).port}"),
                timeoutMillis = 5_000,
            )
            shouldThrow<ApiException> { api.days() }
            requested.get(5, TimeUnit.SECONDS) shouldBe "domain api.tormap.org"
        }
    }
})
