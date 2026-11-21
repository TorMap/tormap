package org.tormap.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.tormap.config.CacheConfig
import org.tormap.database.entity.TorRelayFlag
import org.tormap.database.repository.RelayDetailsRepositoryImpl
import org.tormap.database.repository.RelayLocationRepositoryImpl
import org.tormap.mockRelayDetails
import org.tormap.mockRelayLocation
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiV1ControllerTest(
    private val mockMvc: MockMvc,
    private val relayLocationRepository: RelayLocationRepositoryImpl,
    private val relayDetailsRepository: RelayDetailsRepositoryImpl,
    private val cacheManager: CacheManager,
) : StringSpec({
    val day = LocalDate.of(2024, 3, 5)
    val fingerprintA = "A".repeat(40)

    beforeEach {
        relayLocationRepository.deleteAll()
        relayDetailsRepository.deleteAll()
        listOf(
            CacheConfig.RELAY_LOCATION_DISTINCT_DAYS,
            CacheConfig.RELAY_DAILY_COUNTS,
            CacheConfig.API_V1_RELAYS_PER_DAY,
            CacheConfig.AUTONOMOUS_SYSTEMS_PER_MONTH,
        ).forEach { cacheManager.getCache(it)?.clear() }
    }

    fun saveRelayA(familyId: Long? = null, familyKeys: String? = null): Long {
        relayLocationRepository.save(mockRelayLocation(day, 'A', setOf(TorRelayFlag.Exit, TorRelayFlag.Guard), consensusWeight = 42))
        val details = mockRelayDetails('A').apply {
            month = "2024-03"
            // A loopback address keeps reverse DNS lookups local
            address = "127.0.0.1"
            autonomousSystemNumber = 24940
            this.familyId = familyId
            this.familyKeys = familyKeys
        }
        return relayDetailsRepository.save(details).id!!
    }

    "lists the days with relays" {
        saveRelayA()
        relayLocationRepository.save(mockRelayLocation(day.minusDays(1), 'B'))
        mockMvc.perform(get("/v1/days"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0]").value("2024-03-04"))
            .andExpect(jsonPath("$[1]").value("2024-03-05"))
    }

    "lists the relays of a day with fingerprint and flag names, without internal IDs" {
        saveRelayA(familyId = 7)
        mockMvc.perform(get("/v1/days/2024-03-05/relays"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].fingerprint").value(fingerprintA))
            .andExpect(jsonPath("$[0].detailsId").doesNotExist())
            .andExpect(jsonPath("$[0].nickname").value("A"))
            .andExpect(jsonPath("$[0].country").value("DE"))
            .andExpect(jsonPath("$[0].flags.length()").value(2))
            .andExpect(jsonPath("$[0].flags[0]").value("Exit"))
            .andExpect(jsonPath("$[0].flags[1]").value("Guard"))
            .andExpect(jsonPath("$[0].consensusWeight").value(42))
            .andExpect(jsonPath("$[0].autonomousSystemNumber").value(24940))
            .andExpect(jsonPath("$[0].familyId").value(7))
    }

    "days without relays are not cached" {
        mockMvc.perform(get("/v1/days/2001-01-01/relays"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
        cacheManager.getCache(CacheConfig.API_V1_RELAYS_PER_DAY)!!.get("2001-01-01") shouldBe null
    }

    "days with relays are cached" {
        saveRelayA()
        mockMvc.perform(get("/v1/days/2024-03-05/relays")).andExpect(status().isOk)
        cacheManager.getCache(CacheConfig.API_V1_RELAYS_PER_DAY)!!.get("2024-03-05") shouldNotBe null
    }

    "returns relay details by fingerprint in any case, for the latest month by default" {
        saveRelayA(familyId = 7, familyKeys = "ed25519:one, ed25519:two")
        mockMvc.perform(get("/v1/relays/${fingerprintA.lowercase()}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fingerprint").value(fingerprintA))
            .andExpect(jsonPath("$.month").value("2024-03"))
            .andExpect(jsonPath("$.familyId").value(7))
            .andExpect(jsonPath("$.familyKeys[0]").value("ed25519:one"))
            .andExpect(jsonPath("$.familyKeys[1]").value("ed25519:two"))
            .andExpect(jsonPath("$.id").doesNotExist())
        mockMvc.perform(get("/v1/relays/$fingerprintA").param("month", "2024-03"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fingerprint").value(fingerprintA))
    }

    "an internal details ID is not a relay identifier" {
        val id = saveRelayA()
        mockMvc.perform(get("/v1/relays/$id")).andExpect(status().isBadRequest)
    }

    "an unknown fingerprint or month is not found" {
        saveRelayA()
        mockMvc.perform(get("/v1/relays/${"B".repeat(40)}")).andExpect(status().isNotFound)
        mockMvc.perform(get("/v1/relays/$fingerprintA").param("month", "2024-02")).andExpect(status().isNotFound)
    }

    "a fingerprint lookup without any data is not found" {
        mockMvc.perform(get("/v1/relays/$fingerprintA")).andExpect(status().isNotFound)
    }

    listOf("abc", "A".repeat(39), "A".repeat(41), "G".repeat(40)).forEach { fingerprint ->
        "a malformed fingerprint '$fingerprint' is a bad request" {
            mockMvc.perform(get("/v1/relays/$fingerprint")).andExpect(status().isBadRequest)
        }
    }

    "lists family members by fingerprint and nickname only" {
        saveRelayA(familyId = 7)
        mockMvc.perform(get("/v1/families/7"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").doesNotExist())
            .andExpect(jsonPath("$[0].fingerprint").value(fingerprintA))
            .andExpect(jsonPath("$[0].nickname").value("A"))
            .andExpect(jsonPath("$[0].address").doesNotExist())
    }

    "lists the autonomous systems of a month" {
        saveRelayA()
        mockMvc.perform(get("/v1/autonomous-systems/2024-03"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].number").value(24940))
    }

    "limits daily counts to a range of days" {
        saveRelayA()
        relayLocationRepository.save(mockRelayLocation(day.minusDays(1), 'B'))
        mockMvc.perform(get("/v1/statistics/daily-counts"))
            .andExpect(jsonPath("$.length()").value(2))
        mockMvc.perform(get("/v1/statistics/daily-counts").param("from", "2024-03-05"))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].day").value("2024-03-05"))
        mockMvc.perform(get("/v1/statistics/daily-counts").param("to", "2024-03-04"))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].day").value("2024-03-04"))
        mockMvc.perform(get("/v1/statistics/daily-counts").param("from", "2024-03-06").param("to", "2024-03-05"))
            .andExpect(status().isBadRequest)
    }

    "country usage of a day without estimates is an empty list" {
        mockMvc.perform(get("/v1/country-usage/2024-03-05"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    listOf("abc", "2024-13-01", "2024-02-30", "2024-5-1", "+0002024-05-01").forEach { day ->
        "a malformed day '$day' is a bad request on every endpoint that takes a day" {
            mockMvc.perform(get("/v1/days/$day/relays")).andExpect(status().isBadRequest)
            mockMvc.perform(get("/v1/country-usage/$day")).andExpect(status().isBadRequest)
            mockMvc.perform(get("/v1/statistics/daily-counts").param("from", day)).andExpect(status().isBadRequest)
        }
    }

    listOf("abc", "2024-13", "2024-5", "+002024-05").forEach { month ->
        "a malformed month '$month' is a bad request" {
            mockMvc.perform(get("/v1/autonomous-systems/$month")).andExpect(status().isBadRequest)
            mockMvc.perform(get("/v1/relays/$fingerprintA").param("month", month)).andExpect(status().isBadRequest)
        }
    }

    "the API is read-only" {
        mockMvc.perform(post("/v1/days")).andReturn().response.status shouldBe 403
    }
})
