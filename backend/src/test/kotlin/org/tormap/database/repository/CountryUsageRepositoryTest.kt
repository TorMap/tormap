package org.tormap.database.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.tormap.adapter.dto.CountryUsageRow
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
class CountryUsageRepositoryTest(
    private val countryUsageRepository: CountryUsageRepository,
    private val jdbcTemplate: JdbcTemplate,
) : StringSpec({
    beforeEach {
        jdbcTemplate.update("DELETE FROM country_usage")
    }

    val day = LocalDate.of(2024, 3, 5)

    "findLatestDay is null without data" {
        countryUsageRepository.findLatestDay() shouldBe null
    }

    "stores and returns rows of a day ordered by country" {
        countryUsageRepository.upsertAll(
            listOf(
                CountryUsageRow(day, "FR", 20, null, null, 70),
                CountryUsageRow(day, "DE", 10, 5, 15, 70),
                CountryUsageRow(day.plusDays(1), "DE", 11, 6, 16, 71),
            )
        )
        val rows = countryUsageRepository.findAllByDay(day)
        rows.map { it.country } shouldBe listOf("DE", "FR")
        rows[0].users shouldBe 10
        rows[0].lower shouldBe 5
        rows[0].upper shouldBe 15
        rows[1].lower shouldBe null
        rows[1].upper shouldBe null
        rows[1].frac shouldBe 70
        countryUsageRepository.findLatestDay() shouldBe day.plusDays(1)
    }

    "upserting again updates existing rows" {
        countryUsageRepository.upsertAll(listOf(CountryUsageRow(day, "DE", 10, 5, 15, 60)))
        countryUsageRepository.upsertAll(listOf(CountryUsageRow(day, "DE", 12, 6, 18, 75)))
        val rows = countryUsageRepository.findAllByDay(day)
        rows.size shouldBe 1
        rows[0].users shouldBe 12
        rows[0].frac shouldBe 75
    }

    "returns nothing for a day without data" {
        countryUsageRepository.findAllByDay(day) shouldBe emptyList()
    }
})
