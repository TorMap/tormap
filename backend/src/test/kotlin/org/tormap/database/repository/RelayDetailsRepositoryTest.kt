package org.tormap.database.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.tormap.adapter.dto.RelayIdentifiersDto
import org.tormap.relayDetailsMock

@SpringBootTest
@ActiveProfiles("test")
class RelayDetailsRepositoryTest(
    private val relayDetailsRepository: RelayDetailsRepositoryImpl,
) : StringSpec({
    val relay1 = relayDetailsMock('A')
    val relay2 = relayDetailsMock('B')
    val relay3 = relayDetailsMock('C')

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
        setCommonMonthAndFamilyForRelay1And2()

        relay1.autonomousSystemNumber = 0
        relay2.autonomousSystemNumber = 0
        relay3.autonomousSystemNumber = null

        val months = withContext(Dispatchers.IO) {
            relayDetailsRepository.saveAll(listOf(relay1, relay2, relay3))
            relayDetailsRepository.findDistinctMonthsAndAutonomousSystemNumberNull()
        }

        months shouldContain relay1.month
        months shouldNotContain relay3.month
        months.size shouldBe 1
    }

    "findRelayIdentifiers" {
        relay1.id = 0
        relay2.id = 1
        relay3.id = 2

        val relayIdentifiersList = withContext(Dispatchers.IO) {
            relayDetailsRepository.saveAll(listOf(relay1, relay2, relay3))
            relayDetailsRepository.findRelayIdentifiers(listOf(relay1.id!!, relay2.id!!))
        }

        relayIdentifiersList shouldContain RelayIdentifiersDto(
            id = relay1.id!!,
            fingerprint = relay1.fingerprint,
            nickname = relay1.nickname,
        )
        relayIdentifiersList shouldContain RelayIdentifiersDto(
            id = relay2.id!!,
            fingerprint = relay2.fingerprint,
            nickname = relay2.nickname,
        )
        relayIdentifiersList.size shouldBe 2
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

