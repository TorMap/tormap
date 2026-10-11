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
