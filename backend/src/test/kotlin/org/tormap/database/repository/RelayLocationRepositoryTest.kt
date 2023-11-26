package org.tormap.database.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.tormap.mockRelayLocation
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
class RelayLocationRepositoryTest(
    private val relayLocationRepository: RelayLocationRepositoryImpl,
) : StringSpec({
    beforeEach {
        withContext(Dispatchers.IO) {
            relayLocationRepository.deleteAll()
        }
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
