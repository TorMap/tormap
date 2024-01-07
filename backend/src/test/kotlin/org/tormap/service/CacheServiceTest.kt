package org.tormap.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

    beforeEach {
        relayLocationRepositoryImpl.deleteAll()
        relayLocationDistinctDaysCache?.clear()
        relayLocationsPerDayCache?.clear()
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
            )
        )
        cacheService.cacheRelayLocationDistinctDays().get()
        relayLocationDistinctDaysCache?.get(
            CacheConfig.RELAY_LOCATION_DISTINCT_DAYS_KEY
        )?.get() shouldBe setOf(januaryOne, januaryTwo)
    }

    "cacheRelayLocationsPerDay" {
        relayLocationRepositoryImpl.saveAll(
            listOf(
                mockRelayLocation(januaryOne),
                mockRelayLocation(januaryTwo),
                mockRelayLocation(februaryOne),
            )
        )
        cacheService.cacheRelayLocationsPerDay(setOf("2024-01")).get()
        relayLocationsPerDayCache?.get("2024-01-01") shouldNotBe null
        relayLocationsPerDayCache?.get("2024-01-02") shouldNotBe null
        relayLocationsPerDayCache?.get("2024-02-01") shouldBe null
    }

    "evictRelayLocationsPerDay" {
        relayLocationRepositoryImpl.saveAll(
            listOf(
                mockRelayLocation(januaryOne),
                mockRelayLocation(februaryOne),
            )
        )
        cacheService.cacheRelayLocationsPerDay(setOf("2024-01", "2024-02")).get()
        cacheService.evictRelayLocationsPerDay(setOf("2024-01")).get()
        relayLocationsPerDayCache?.get("2024-01-01") shouldBe null
        relayLocationsPerDayCache?.get("2024-02-01") shouldNotBe null
    }
})
