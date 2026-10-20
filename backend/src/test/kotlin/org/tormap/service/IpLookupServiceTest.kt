package org.tormap.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class IpLookupServiceTest(
    private val ipLookupService: IpLookupService
) : StringSpec({
    val hetznerIPAddress = "94.130.58.99"
    val hetznerAutonomousSystemNumber = 24940
    val germanyIsoCountryCode = "DE"

    "lookupLocation" {
        ipLookupService.lookupLocation(hetznerIPAddress)?.countryCode shouldBe germanyIsoCountryCode
    }

    "lookupAutonomousSystem" {
        ipLookupService.lookupAutonomousSystem(hetznerIPAddress)?.autonomousSystemNumber() shouldBe hetznerAutonomousSystemNumber
    }

    "returns null for invalid IP address" {
        ipLookupService.lookupLocation("invalid IP address") shouldBe null
        ipLookupService.lookupAutonomousSystem("invalid IP address") shouldBe null
    }

    "returns null for private IP address" {
        listOf("10.0.0.1", "172.16.0.1", "172.31.255.255", "192.168.1.1").forEach {
            ipLookupService.lookupLocation(it) shouldBe null
            ipLookupService.lookupAutonomousSystem(it) shouldBe null
        }
    }

    "neighbours of private ranges are looked up" {
        // Hetzner and Google, right next to 172.16.0.0/12 and 192.168.0.0/16
        ipLookupService.lookupLocation("172.15.255.255").shouldNotBeNull()
        ipLookupService.lookupLocation("172.32.0.1").shouldNotBeNull()
        ipLookupService.lookupLocation("192.169.0.1").shouldNotBeNull()
    }

    "returns null for addresses without a database entry" {
        listOf("127.0.0.1", "0.0.0.1").forEach {
            ipLookupService.lookupLocation(it) shouldBe null
            ipLookupService.lookupAutonomousSystem(it) shouldBe null
        }
    }

    "IPv6 addresses are not looked up" {
        ipLookupService.lookupLocation("2001:4860:4860::8888") shouldBe null
    }
})
