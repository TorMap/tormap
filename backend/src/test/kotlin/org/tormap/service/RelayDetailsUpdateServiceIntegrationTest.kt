package org.tormap.service

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.tormap.database.repository.RelayDetailsRepositoryImpl
import org.tormap.mockRelayDetails
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@SpringBootTest
@ActiveProfiles("test")
class RelayDetailsUpdateServiceIntegrationTest(
    private val relayDetailsUpdateService: RelayDetailsUpdateService,
    private val relayDetailsRepository: RelayDetailsRepositoryImpl,
) : StringSpec({
    val month = "2023-05"
    val fingerprint = { letter: Char -> letter.toString().repeat(40) }

    beforeEach {
        relayDetailsRepository.deleteAll()
    }

    fun relay(letter: Char, familyEntries: String?, address: String = "172.104.114.113") =
        mockRelayDetails(letter).apply {
            this.month = month
            this.familyEntries = familyEntries
            this.address = address
        }

    fun byNickname() = relayDetailsRepository.findAll().associateBy { it.nickname }

    "relays that list each other form a family" {
        relayDetailsRepository.saveAll(
            listOf(
                relay('A', "\$${fingerprint('B')}, \$${fingerprint('C')}"),
                relay('B', "\$${fingerprint('A')}"),
                // C does not confirm A
                relay('C', "\$${fingerprint('D')}"),
                // D and E form a second family, the invalid entry is skipped
                relay('D', "\$${fingerprint('E')}, not a valid entry!"),
                relay('E', "\$${fingerprint('D')}"),
                relay('F', null),
            )
        )

        relayDetailsUpdateService.computeFamilies(setOf(month)).get(30, TimeUnit.SECONDS)

        val relays = byNickname()
        relays.getValue("A").familyId.shouldNotBeNull()
        relays.getValue("A").familyId shouldBe relays.getValue("B").familyId
        relays.getValue("D").familyId.shouldNotBeNull()
        relays.getValue("D").familyId shouldBe relays.getValue("E").familyId
        relays.getValue("A").familyId shouldNotBe relays.getValue("D").familyId
        relays.getValue("C").familyId.shouldBeNull()
        relays.getValue("F").familyId.shouldBeNull()
    }

    "families that are connected through a member are merged" {
        relayDetailsRepository.saveAll(
            listOf(
                relay('A', "\$${fingerprint('B')}"),
                relay('B', "\$${fingerprint('A')}, \$${fingerprint('C')}"),
                relay('C', "\$${fingerprint('B')}"),
            )
        )

        relayDetailsUpdateService.computeFamilies(setOf(month)).get(30, TimeUnit.SECONDS)

        byNickname().values.map { it.familyId }.toSet().single().shouldNotBeNull()
    }

    "computing the families again replaces the previous families" {
        relayDetailsRepository.saveAll(listOf(relay('A', "\$${fingerprint('B')}"), relay('B', "\$${fingerprint('A')}")))
        relayDetailsUpdateService.computeFamilies(setOf(month)).get(30, TimeUnit.SECONDS)
        val firstFamilyId = byNickname().getValue("A").familyId

        val relayA = byNickname().getValue("A")
        relayA.familyEntries = null
        relayDetailsRepository.save(relayA)
        relayDetailsUpdateService.computeFamilies(setOf(month)).get(30, TimeUnit.SECONDS)

        firstFamilyId.shouldNotBeNull()
        byNickname().getValue("A").familyId.shouldBeNull()
        byNickname().getValue("B").familyId.shouldBeNull()
    }

    "computeAllMissingFamiliesAndEvictCache computes families of months without any" {
        relayDetailsRepository.saveAll(listOf(relay('A', "\$${fingerprint('B')}"), relay('B', "\$${fingerprint('A')}")))

        relayDetailsUpdateService.computeAllMissingFamiliesAndEvictCache()

        eventually(10.seconds) {
            byNickname().getValue("A").familyId.shouldNotBeNull()
        }
    }

    "lookupAllMissingAutonomousSystems fills in the autonomous system" {
        relayDetailsRepository.saveAll(
            listOf(
                relay('A', null, address = "94.130.58.99"),
                // Private addresses have no autonomous system
                relay('B', null, address = "10.0.0.1"),
            )
        )

        relayDetailsUpdateService.lookupAllMissingAutonomousSystems()

        eventually(10.seconds) {
            byNickname().getValue("A").autonomousSystemNumber shouldBe 24940
        }
        byNickname().getValue("A").autonomousSystemName.shouldNotBeNull()
        byNickname().getValue("B").autonomousSystemNumber.shouldBeNull()
    }
})
