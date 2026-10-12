package org.tormap.controller

import io.kotest.core.spec.style.StringSpec
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.cache.CacheManager
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.tormap.adapter.dto.CountryUsageRow
import org.tormap.config.CacheConfig
import org.tormap.database.repository.CountryUsageRepository
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CountryUsageControllerTest(
    private val mockMvc: MockMvc,
    private val countryUsageRepository: CountryUsageRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val cacheManager: CacheManager,
) : StringSpec({
    beforeEach {
        jdbcTemplate.update("DELETE FROM country_usage")
        cacheManager.getCache(CacheConfig.COUNTRY_USAGE_PER_DAY)?.clear()
    }

    "returns the usage of the requested day" {
        countryUsageRepository.upsertAll(
            listOf(
                CountryUsageRow(LocalDate.of(2024, 3, 5), "DE", 110, 90, 130, 61),
                CountryUsageRow(LocalDate.of(2024, 3, 6), "DE", 1, null, null, 61),
            )
        )
        mockMvc.perform(get("/relay/country-usage/day/2024-03-05"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].country").value("DE"))
            .andExpect(jsonPath("$[0].users").value(110))
            .andExpect(jsonPath("$[0].lower").value(90))
            .andExpect(jsonPath("$[0].upper").value(130))
            .andExpect(jsonPath("$[0].frac").value(61))
    }

    "returns an empty list for days without estimates" {
        mockMvc.perform(get("/relay/country-usage/day/2007-10-28"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    "rejects days that are not a date" {
        listOf("yesterday", "2024-13-40", "2024-3-5", "1%27%20OR%201=1").forEach { day ->
            mockMvc.perform(get("/relay/country-usage/day/$day"))
                .andExpect(status().isBadRequest)
        }
    }
})
