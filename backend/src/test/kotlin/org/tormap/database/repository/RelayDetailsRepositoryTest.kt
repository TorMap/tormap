package org.tormap.database.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.tormap.mockRelayDetails

@SpringBootTest
@ActiveProfiles("test")
class RelayDetailsRepositoryTest(
    private val relayDetailsRepository: RelayDetailsRepositoryImpl,
) : StringSpec({
    beforeEach {
        relayDetailsRepository.deleteAll()
    }

    val relay1 = mockRelayDetails('A')
    val relay2 = mockRelayDetails('B')
    val relay3 = mockRelayDetails('C')

    fun setCommonMonthAndFamilyForRelay1And2() {
        relay1.month = "2023-01"
        relay2.month = "2023-01"
        relay3.month = "2023-02"

        relay1.familyId = 0L
        relay2.familyId = 0L
        relay3.familyId = 1L
    }

    "findDistinctMonthFamilyMemberCount" {
        setCommonMonthAndFamilyForRelay1And2()

        relayDetailsRepository.saveAll(listOf(relay1, relay2, relay3))
        val monthFamilyCounts = relayDetailsRepository.findDistinctMonthFamilyMemberCount()

        monthFamilyCounts.find { it.month == relay1.month }?.count shouldBe 2
        monthFamilyCounts.find { it.month == relay3.month }?.count shouldBe 1
    }

    "findDistinctMonthsAndAutonomousSystemNumberNull" {
        relay1.month = "2023-01"
        relay1.autonomousSystemNumber = null
        relay2.month = "2023-02"
        relay2.autonomousSystemNumber = null
        relay3.month = "2023-03"
        relay3.autonomousSystemNumber = 0

        relayDetailsRepository.saveAll(listOf(relay1, relay2, relay3))
        val months = relayDetailsRepository.findDistinctMonthsAndAutonomousSystemNumberNull()

        months shouldBe setOf(relay1.month, relay2.month)
    }

    "findRelayIdentifiers" {
        val savedRelay1 = relayDetailsRepository.save(relay1)
        val savedRelay2 = relayDetailsRepository.save(relay2)
        relayDetailsRepository.save(relay3)


        val relayIdentifiersList =
            relayDetailsRepository.findRelayIdentifiers(listOf(savedRelay1.id!!, savedRelay2.id!!))

        relayIdentifiersList.find { relayIdentifiers ->
            relayIdentifiers.id == savedRelay1.id!!
                && relayIdentifiers.fingerprint == savedRelay1.fingerprint
                && relayIdentifiers.nickname == savedRelay1.nickname
        }.shouldNotBeNull()

        relayIdentifiersList.find { relayIdentifiers ->
            relayIdentifiers.id == savedRelay2.id!!
                && relayIdentifiers.fingerprint == savedRelay2.fingerprint
                && relayIdentifiers.nickname == savedRelay2.nickname
        }.shouldNotBeNull()

        relayIdentifiersList.size shouldBe 2
    }

    "findFamilyIdentifiers" {
        setCommonMonthAndFamilyForRelay1And2()

        relayDetailsRepository.saveAll(listOf(relay1, relay2, relay3))

        val familyIdentifiers = relayDetailsRepository.findFamilyIdentifiers(listOf(relay1.familyId!!)).first()
        familyIdentifiers.id shouldBe relay1.familyId!!
        familyIdentifiers.memberCount shouldBe 2
        familyIdentifiers.nicknames shouldBe "${relay1.nickname}, ${relay2.nickname}"
        familyIdentifiers.autonomousSystems shouldBe "${relay1.autonomousSystemName}, ${relay2.autonomousSystemName}"
    }

    "clearFamiliesFromMonth" {
        setCommonMonthAndFamilyForRelay1And2()

        relayDetailsRepository.saveAll(listOf(relay1, relay2, relay3))
        val updatedEntryCount = relayDetailsRepository.clearFamiliesFromMonth(relay1.month)

        updatedEntryCount shouldBe 2
        relayDetailsRepository.findAllByFamilyId(relay1.familyId!!).size shouldBe 0
    }
})
