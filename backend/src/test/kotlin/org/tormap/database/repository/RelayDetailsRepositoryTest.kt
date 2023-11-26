package org.tormap.database.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.tormap.mockRelayDetails

@SpringBootTest
@ActiveProfiles("test")
class RelayDetailsRepositoryTest(
    private val relayDetailsRepository: RelayDetailsRepositoryImpl,
) : StringSpec({
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

    beforeEach {
        withContext(Dispatchers.IO) {
            relayDetailsRepository.deleteAll()
        }
    }

    "findDistinctMonthFamilyMemberCount" {
        setCommonMonthAndFamilyForRelay1And2()

        val monthFamilyCounts = withContext(Dispatchers.IO) {
            relayDetailsRepository.saveAll(listOf(relay1, relay2, relay3))
            relayDetailsRepository.findDistinctMonthFamilyMemberCount()
        }

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

        val months = withContext(Dispatchers.IO) {
            relayDetailsRepository.saveAll(listOf(relay1, relay2, relay3))
            relayDetailsRepository.findDistinctMonthsAndAutonomousSystemNumberNull()
        }

        months shouldBe setOf(relay1.month, relay2.month)
    }

    "findRelayIdentifiers" {
        relay1.id = 0
        relay2.id = 1
        relay3.id = 2

        val relayIdentifiersList = withContext(Dispatchers.IO) {
            relayDetailsRepository.saveAll(listOf(relay1, relay2, relay3))
            relayDetailsRepository.findRelayIdentifiers(listOf(relay1.id!!, relay2.id!!))
        }

        // TODO
//        relayIdentifiersList.find { relayIdentifiers ->
//            relayIdentifiers.id == relay1.id!!
//                && relayIdentifiers.fingerprint == relay1.fingerprint
//                && relayIdentifiers.nickname == relay1.nickname
//        }.shouldNotBeNull()
//
//        relayIdentifiersList.find { relayIdentifiers ->
//            relayIdentifiers.id == relay2.id!!
//                && relayIdentifiers.fingerprint == relay2.fingerprint
//                && relayIdentifiers.nickname == relay2.nickname
//        }.shouldNotBeNull()
//
//        relayIdentifiersList.size shouldBe 2
    }

    "findFamilyIdentifiers" {
        setCommonMonthAndFamilyForRelay1And2()

        val familyIdentifiers = withContext(Dispatchers.IO) {
            relayDetailsRepository.saveAll(listOf(relay1, relay2, relay3))
            relayDetailsRepository.findFamilyIdentifiers(listOf(relay1.familyId!!))
        }.first()
        familyIdentifiers.id shouldBe relay1.familyId!!
        familyIdentifiers.memberCount shouldBe 2
        familyIdentifiers.nicknames shouldBe "${relay1.nickname}, ${relay2.nickname}"
        familyIdentifiers.autonomousSystems shouldBe "${relay1.autonomousSystemName}, ${relay2.autonomousSystemName}"
    }

    "clearFamiliesFromMonth" {
        setCommonMonthAndFamilyForRelay1And2()

        val updatedEntryCount = withContext(Dispatchers.IO) {
            relayDetailsRepository.saveAll(listOf(relay1, relay2, relay3))
            relayDetailsRepository.clearFamiliesFromMonth(relay1.month)
        }
        updatedEntryCount shouldBe 2
        withContext(Dispatchers.IO) {
            relayDetailsRepository.findAllByFamilyId(relay1.familyId!!)
        }.size shouldBe 0
    }
})
