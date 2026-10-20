package org.tormap.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.tormap.config.CacheConfig
import org.tormap.database.entity.TorRelayFlag
import org.tormap.database.repository.RelayLocationRepositoryImpl
import org.tormap.mockRelayLocation
import org.tormap.service.CacheService
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RelayStatisticsControllerTest(
    private val mockMvc: MockMvc,
    private val relayLocationRepository: RelayLocationRepositoryImpl,
    private val cacheManager: CacheManager,
    private val cacheService: CacheService,
) : StringSpec({
    val day = LocalDate.of(2024, 3, 5)

    beforeEach {
        relayLocationRepository.deleteAll()
        cacheManager.getCache(CacheConfig.RELAY_DAILY_COUNTS)?.clear()
    }

    "returns the relay counts per day" {
        relayLocationRepository.saveAll(
            listOf(
                mockRelayLocation(day, 'A', setOf(TorRelayFlag.Exit)),
                mockRelayLocation(day, 'B', setOf(TorRelayFlag.Guard)),
                mockRelayLocation(day, 'C'),
            )
        )
        mockMvc.perform(get("/relay/statistics/daily-counts"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].day").value("2024-03-05"))
            .andExpect(jsonPath("$[0].total").value(3))
            .andExpect(jsonPath("$[0].exit").value(1))
            .andExpect(jsonPath("$[0].guard").value(1))
            .andExpect(jsonPath("$[0].other").value(1))
    }

    "serves the cached counts until the cache is refreshed" {
        mockMvc.perform(get("/relay/statistics/daily-counts")).andExpect(jsonPath("$.length()").value(0))
        relayLocationRepository.save(mockRelayLocation(day, 'A'))
        mockMvc.perform(get("/relay/statistics/daily-counts")).andExpect(jsonPath("$.length()").value(0))

        cacheService.cacheRelayDailyCounts().get()

        mockMvc.perform(get("/relay/statistics/daily-counts")).andExpect(jsonPath("$.length()").value(1))
    }

    "GET / names the application" {
        val response = mockMvc.perform(get("/")).andExpect(status().isOk).andReturn().response
        response.contentAsString shouldStartWith "TorMap "
        // Swagger is disabled by default, so the docs are not linked
        response.contentAsString.contains("Docs:") shouldBe false
    }
})
