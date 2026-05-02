package org.tormap.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReverseDnsLookupServiceTest : StringSpec({
    "lookupHostNames separates verified and unverified host names" {
        val reverseDnsResolver = LookupServiceFakeReverseDnsResolver(
            ptrRecordsByIpAddress = mapOf(
                "1.2.3.4" to listOf(
                    "verified.example.",
                    "verified.example.",
                    "unverified.example.",
                )
            ),
            addressesByHostName = mapOf(
                "verified.example" to listOf("1.2.3.4"),
                "unverified.example" to listOf("5.6.7.8"),
            ),
        )
        val reverseDnsLookupService = ReverseDnsLookupService(reverseDnsResolver)

        val result = reverseDnsLookupService.lookupHostNames("1.2.3.4")

        result.verifiedHostNames shouldBe listOf("verified.example")
        result.unverifiedHostNames shouldBe listOf("unverified.example")
    }

    "lookupHostNames returns empty lists when no PTR records exist" {
        val reverseDnsResolver = LookupServiceFakeReverseDnsResolver()
        val reverseDnsLookupService = ReverseDnsLookupService(reverseDnsResolver)

        val result = reverseDnsLookupService.lookupHostNames("1.2.3.4")

        result shouldBe ReverseDnsLookupResult()
        reverseDnsResolver.lookupAddressesCallCount shouldBe 0
    }
})

private class LookupServiceFakeReverseDnsResolver(
    private val ptrRecordsByIpAddress: Map<String, List<String>> = emptyMap(),
    private val addressesByHostName: Map<String, List<String>> = emptyMap(),
) : ReverseDnsResolver {
    var lookupAddressesCallCount = 0

    override fun lookupPtrRecords(ipAddress: String): List<String> = ptrRecordsByIpAddress[ipAddress] ?: emptyList()

    override fun lookupAddresses(hostName: String): List<String> {
        lookupAddressesCallCount++
        return addressesByHostName[hostName] ?: emptyList()
    }
}
