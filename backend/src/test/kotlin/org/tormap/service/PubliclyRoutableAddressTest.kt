package org.tormap.service

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.net.InetAddress

class PubliclyRoutableAddressTest : StringSpec({
    // Literal addresses, so no DNS lookup takes place
    fun routable(address: String) = InetAddress.getByName(address).isPubliclyRoutable()

    "public addresses are routable" {
        listOf("1.1.1.1", "94.130.58.99", "100.63.255.255", "100.128.0.1", "2001:4860:4860::8888").forEach {
            routable(it) shouldBe true
        }
    }

    "internal and special addresses are not routable" {
        listOf(
            "127.0.0.1", "::1", // loopback
            "169.254.1.1", "fe80::1", // link local
            "10.0.0.1", "172.16.0.1", "192.168.1.1", // private
            "fc00::1", "fd12:3456::1", // unique local
            "224.0.0.1", "ff02::1", // multicast
            "0.0.0.0", "::", // any
            "0.1.2.3", // "this" network
            "100.64.0.1", "100.127.255.255", // carrier-grade NAT
        ).forEach {
            withClue(it) { routable(it) shouldBe false }
        }
    }

    "DNSJava resolver drops unresolvable names without throwing" {
        val resolver = DNSJavaReverseDnsResolver()
        resolver.lookupAddresses("not a valid host name..") shouldBe emptyList()
        resolver.lookupPtrRecords("not an address") shouldBe emptyList()
    }
})
