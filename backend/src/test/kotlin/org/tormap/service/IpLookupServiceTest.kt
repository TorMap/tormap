package org.tormap.service

import io.kotest.core.spec.style.StringSpec
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
        ipLookupService.lookupAutonomousSystem(hetznerIPAddress)?.autonomousSystemNumber shouldBe hetznerAutonomousSystemNumber
    }
})
