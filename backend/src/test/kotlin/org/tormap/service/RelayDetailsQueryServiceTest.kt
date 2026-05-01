package org.tormap.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.tormap.adapter.controller.exception.RelayNotFoundException
import org.tormap.database.entity.RelayDetails
import org.tormap.mockRelayDetails

class RelayDetailsQueryServiceTest : StringSpec({
    "getRelay enriches relay details with confirmed family members and reverse DNS names" {
        val relay = mockRelayDetails('A').apply {
            id = 1L
            familyId = 11L
            address = "1.2.3.4"
        }
        val familyMember = mockRelayDetails('B').apply {
            id = 2L
            familyId = 11L
        }

        val relayDetailsQueryService = RelayDetailsQueryService(
            relayDetailsLookup = FakeRelayDetailsLookup(
                relaysById = mapOf(1L to relay),
                relaysByFamilyId = mapOf(11L to listOf(relay, familyMember)),
            ),
            reverseDnsLookupService = ReverseDnsLookupService(
                reverseDnsResolver = QueryServiceFakeReverseDnsResolver(
                    ptrRecordsByIpAddress = mapOf(
                        "1.2.3.4" to listOf("verified.example.", "unverified.example.")
                    ),
                    addressesByHostName = mapOf(
                        "verified.example" to listOf("1.2.3.4"),
                        "unverified.example" to listOf("5.6.7.8"),
                    ),
                )
            ),
        )

        val response = relayDetailsQueryService.getRelay(1L)

        response.id shouldBe 1L
        response.confirmedFamilyMembers.map { it.id } shouldBe listOf(2L)
        response.confirmedFamilyMembers.map { it.nickname } shouldBe listOf(familyMember.nickname)
        response.verifiedHostNames shouldBe listOf("verified.example")
        response.unverifiedHostNames shouldBe listOf("unverified.example")
    }

    "getRelay returns empty confirmed family members when relay has no family" {
        val relay = mockRelayDetails('A').apply {
            id = 1L
            familyId = null
            address = "1.2.3.4"
        }

        val relayDetailsQueryService = RelayDetailsQueryService(
            relayDetailsLookup = FakeRelayDetailsLookup(relaysById = mapOf(1L to relay)),
            reverseDnsLookupService = ReverseDnsLookupService(QueryServiceFakeReverseDnsResolver()),
        )

        val response = relayDetailsQueryService.getRelay(1L)

        response.confirmedFamilyMembers shouldBe emptyList()
    }

    "getRelay throws when relay does not exist" {
        val relayDetailsQueryService = RelayDetailsQueryService(
            relayDetailsLookup = FakeRelayDetailsLookup(),
            reverseDnsLookupService = ReverseDnsLookupService(QueryServiceFakeReverseDnsResolver()),
        )

        shouldThrow<RelayNotFoundException> {
            relayDetailsQueryService.getRelay(99L)
        }
    }
})

private class FakeRelayDetailsLookup(
    private val relaysById: Map<Long, RelayDetails> = emptyMap(),
    private val relaysByFamilyId: Map<Long, List<RelayDetails>> = emptyMap(),
) : RelayDetailsLookup {
    override fun findById(id: Long): RelayDetails? = relaysById[id]

    override fun findAllByFamilyId(familyId: Long): List<RelayDetails> = relaysByFamilyId[familyId] ?: emptyList()
}

private class QueryServiceFakeReverseDnsResolver(
    private val ptrRecordsByIpAddress: Map<String, List<String>> = emptyMap(),
    private val addressesByHostName: Map<String, List<String>> = emptyMap(),
) : ReverseDnsResolver {
    override fun lookupPtrRecords(ipAddress: String): List<String> = ptrRecordsByIpAddress[ipAddress] ?: emptyList()

    override fun lookupAddresses(hostName: String): List<String> = addressesByHostName[hostName] ?: emptyList()
}
