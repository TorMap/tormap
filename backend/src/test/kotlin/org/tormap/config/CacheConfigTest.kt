package org.tormap.config

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class CacheConfigTest(
    private val cacheManager: CacheManager,
): StringSpec({
    val relayLocationDistinctDaysCache = cacheManager.getCache(CacheConfig.RELAY_LOCATION_DISTINCT_DAYS)
    val relayLocationsPerDayCache = cacheManager.getCache(CacheConfig.RELAY_LOCATIONS_PER_DAY)

    beforeEach {
        relayLocationDistinctDaysCache?.clear()
        relayLocationsPerDayCache?.clear()
    }

    "caches exists" {
        relayLocationDistinctDaysCache shouldNotBe null
        relayLocationsPerDayCache shouldNotBe null
    }
})
