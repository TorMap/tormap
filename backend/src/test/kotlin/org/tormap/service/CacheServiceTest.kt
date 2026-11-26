package org.tormap.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles
import org.tormap.config.CacheConfig
import org.tormap.database.repository.RelayLocationRepositoryImpl
import org.tormap.mockRelayLocation
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
class CacheServiceTest(
    private val cacheService: CacheService,
    private val relayLocationRepositoryImpl: RelayLocationRepositoryImpl,
    private val cacheManager: CacheManager,
) : StringSpec({
    val relayLocationDistinctDaysCache = cacheManager.getCache(CacheConfig.RELAY_LOCATION_DISTINCT_DAYS)
    val relayLocationsPerDayCache = cacheManager.getCache(CacheConfig.RELAY_LOCATIONS_PER_DAY)
    val relayDailyCountsCache = cacheManager.getCache(CacheConfig.RELAY_DAILY_COUNTS)
    val apiV1RelaysPerDayCache = cacheManager.getCache(CacheConfig.API_V1_RELAYS_PER_DAY)!!

    beforeEach {
        relayLocationRepositoryImpl.deleteAll()
        relayDailyCountsCache?.clear()
        relayLocationDistinctDaysCache?.clear()
        relayLocationsPerDayCache?.clear()
        apiV1RelaysPerDayCache.clear()
    }

    val januaryOne = LocalDate.of(2024, 1, 1)
    val januaryTwo = LocalDate.of(2024, 1, 2)
    val februaryOne = LocalDate.of(2024, 2, 1)

    "cacheRelayLocationDistinctDays" {
        relayLocationRepositoryImpl.saveAll(
            listOf(
                mockRelayLocation(januaryOne),
                mockRelayLocation(januaryTwo, 'A'),
                mockRelayLocation(januaryTwo, 'B'),
            ),
        )
        cacheService.cacheRelayLocationDistinctDays().get()
        relayLocationDistinctDaysCache?.get(
            CacheConfig.RELAY_LOCATION_DISTINCT_DAYS_KEY,
        )?.get() shouldBe setOf(januaryOne, januaryTwo)
    }

    "cacheRelayDailyCounts" {
        relayLocationRepositoryImpl.saveAll(
            listOf(
                mockRelayLocation(januaryOne),
                mockRelayLocation(januaryTwo, 'A'),
                mockRelayLocation(januaryTwo, 'B'),
            ),
        )
        cacheService.cacheRelayDailyCounts().get()
        val cached = relayDailyCountsCache?.get(CacheConfig.RELAY_DAILY_COUNTS_KEY)?.get() as List<*>
        cached.size shouldBe 2
    }

    "cacheRelayLocationsPerDay warms the API v1 day cache the map reads" {
        relayLocationRepositoryImpl.saveAll(
            listOf(
                mockRelayLocation(januaryOne),
                mockRelayLocation(januaryTwo),
                mockRelayLocation(februaryOne),
            ),
        )
        cacheService.cacheRelayLocationsPerDay(setOf("2024-01")).get()
        val cached = apiV1RelaysPerDayCache.get("2024-01-01")?.get() as ApiV1RelaysOfDayService.RelaysOfDay
        cached.relays.size shouldBe 1
        cached.eTag shouldStartWith "W/\""
        apiV1RelaysPerDayCache.get("2024-01-02") shouldNotBe null
        apiV1RelaysPerDayCache.get("2024-01-03") shouldBe null
        apiV1RelaysPerDayCache.get("2024-02-01") shouldBe null
    }

    "evictRelayLocationsPerDay" {
        relayLocationRepositoryImpl.saveAll(
            listOf(
                mockRelayLocation(januaryOne),
                mockRelayLocation(februaryOne),
            ),
        )
        cacheService.cacheRelayLocationsPerDay(setOf("2024-01", "2024-02")).get()
        cacheService.evictRelayLocationsPerDay(setOf("2024-01")).get()
        apiV1RelaysPerDayCache.get("2024-01-01") shouldBe null
        apiV1RelaysPerDayCache.get("2024-02-01") shouldNotBe null
    }

    "the old day cache (TorMap Lite) is cleared for reprocessed and evicted days" {
        relayLocationsPerDayCache!!.put("2024-01-01", listOf<Any>())
        relayLocationsPerDayCache.put("2024-02-01", listOf<Any>())
        relayLocationsPerDayCache.put("2024-03-01", listOf<Any>())
        cacheService.cacheRelayLocationsPerDay(setOf("2024-01")).get()
        cacheService.evictRelayLocationsPerDay(setOf("2024-02")).get()
        relayLocationsPerDayCache.get("2024-01-01") shouldBe null
        relayLocationsPerDayCache.get("2024-02-01") shouldBe null
        relayLocationsPerDayCache.get("2024-03-01") shouldNotBe null
    }

    "reprocessing a day that lost its relays clears its entry" {
        apiV1RelaysPerDayCache.put("2024-01-03", ApiV1RelaysOfDayService.RelaysOfDay(listOf(), "W/\"x\""))
        cacheService.cacheRelayLocationsPerDay(setOf("2024-01")).get()
        apiV1RelaysPerDayCache.get("2024-01-03") shouldBe null
    }

    "the API v1 families of reprocessed and evicted months are cleared, because their IDs change" {
        val familiesCache = cacheManager.getCache(CacheConfig.API_V1_FAMILIES_PER_MONTH)!!
        familiesCache.put("2024-01", listOf<Any>())
        familiesCache.put("2024-02", listOf<Any>())
        familiesCache.put("2024-03", listOf<Any>())
        cacheService.cacheRelayLocationsPerDay(setOf("2024-01")).get()
        cacheService.evictRelayLocationsPerDay(setOf("2024-02")).get()
        familiesCache.get("2024-01") shouldBe null
        familiesCache.get("2024-02") shouldBe null
        familiesCache.get("2024-03") shouldNotBe null
    }
})
