package org.tormap.database.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.tormap.database.entity.TorRelayFlag
import org.tormap.mockRelayLocation
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
class RelayLocationRepositoryTest(
    private val relayLocationRepository: RelayLocationRepositoryImpl,
) : StringSpec({
    beforeEach {
        relayLocationRepository.deleteAll()
    }

    val firstDayOfYear = LocalDate.ofYearDay(2023, 1)
    val secondDayOfYear = LocalDate.ofYearDay(2023, 2)

    "findDistinctDays" {
        relayLocationRepository.saveAll(
            listOf(
                mockRelayLocation(secondDayOfYear, 'A'),
                mockRelayLocation(firstDayOfYear, 'B'),
                mockRelayLocation(secondDayOfYear, 'C'),
            )
        )
        relayLocationRepository.findDistinctDays() shouldBe setOf(firstDayOfYear, secondDayOfYear)
    }

    "consensus and guard weights are stored and returned with the relay location" {
        relayLocationRepository.save(
            mockRelayLocation(
                firstDayOfYear, 'A', setOf(TorRelayFlag.Running, TorRelayFlag.Valid, TorRelayFlag.Guard),
                consensusWeight = 2000, bandwidthWeights = mapOf("Wgg" to 5000),
            )
        )
        val stored = relayLocationRepository.findAll().single()
        stored.consensusWeight shouldBe 2000
        stored.guardWeight shouldBe 1000
        relayLocationRepository.findAllUsingDay(firstDayOfYear).single().consensusWeight shouldBe 2000
    }

    "relay locations without consensus entry weight have none" {
        relayLocationRepository.save(mockRelayLocation(firstDayOfYear, 'A'))
        val stored = relayLocationRepository.findAll().single()
        stored.consensusWeight shouldBe null
        stored.guardWeight shouldBe null
        relayLocationRepository.findDistinctFingerprintsWithoutConsensusWeightByDay(firstDayOfYear) shouldBe
            setOf("A".repeat(40))
    }

    "fillMissingConsensusWeights only fills relays of the day that have no weight yet" {
        relayLocationRepository.saveAll(
            listOf(
                mockRelayLocation(firstDayOfYear, 'A'),
                mockRelayLocation(firstDayOfYear, 'B', consensusWeight = 7),
                mockRelayLocation(firstDayOfYear, 'C'),
                mockRelayLocation(secondDayOfYear, 'A'),
            )
        )
        val updated = relayLocationRepository.fillMissingConsensusWeights(
            firstDayOfYear,
            listOf('A', 'B', 'X').joinToString(",") { it.toString().repeat(40) },
            "100,200,300",
            "50,-1,10",
        )
        updated shouldBe 1
        val byKey = relayLocationRepository.findAll().associateBy { it.day to it.fingerprint[0] }
        byKey[firstDayOfYear to 'A']!!.consensusWeight shouldBe 100
        byKey[firstDayOfYear to 'A']!!.guardWeight shouldBe 50
        // Existing weights are kept
        byKey[firstDayOfYear to 'B']!!.consensusWeight shouldBe 7
        byKey[firstDayOfYear to 'C']!!.consensusWeight shouldBe null
        byKey[secondDayOfYear to 'A']!!.consensusWeight shouldBe null
    }

    "fillMissingConsensusWeights stores an unknown guard weight as null" {
        relayLocationRepository.save(mockRelayLocation(firstDayOfYear, 'A'))
        relayLocationRepository.fillMissingConsensusWeights(firstDayOfYear, "A".repeat(40), "100", "-1")
        val stored = relayLocationRepository.findAll().single()
        stored.consensusWeight shouldBe 100
        stored.guardWeight shouldBe null
    }

    "findDailyRelayCounts splits relays into exit, guard and other" {
        relayLocationRepository.saveAll(
            listOf(
                mockRelayLocation(firstDayOfYear, 'A', setOf(TorRelayFlag.Running, TorRelayFlag.Exit)),
                // An exit that is also a guard counts as exit only
                mockRelayLocation(firstDayOfYear, 'B', setOf(TorRelayFlag.Exit, TorRelayFlag.Guard)),
                mockRelayLocation(firstDayOfYear, 'C', setOf(TorRelayFlag.Running, TorRelayFlag.Guard)),
                mockRelayLocation(firstDayOfYear, 'D', setOf(TorRelayFlag.Running, TorRelayFlag.Fast)),
                mockRelayLocation(firstDayOfYear, 'E'),
                mockRelayLocation(secondDayOfYear, 'A', setOf(TorRelayFlag.Guard)),
            )
        )
        val counts = relayLocationRepository.findDailyRelayCounts()
        counts.map { it.day } shouldBe listOf(firstDayOfYear, secondDayOfYear)
        counts[0].total shouldBe 5
        counts[0].exit shouldBe 2
        counts[0].guard shouldBe 1
        counts[0].other shouldBe 2
        counts[1].total shouldBe 1
        counts[1].guard shouldBe 1
        counts[1].exit shouldBe 0
    }

    "findDailyRelayCounts uses the flag ordinals of the query" {
        // The query matches the Exit and Guard ordinals as literals, so reordering the enum must fail here
        TorRelayFlag.Exit.ordinal shouldBe 5
        TorRelayFlag.Guard.ordinal shouldBe 7
    }

    "findAllUsingDay" {
        relayLocationRepository.saveAll(
            listOf(
                mockRelayLocation(firstDayOfYear, 'A'),
                mockRelayLocation(secondDayOfYear, 'B'),
            )
        )
        relayLocationRepository.findAllUsingDay(firstDayOfYear).size shouldBe 1
    }
})
