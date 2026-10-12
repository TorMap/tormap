package org.tormap.config

import com.github.benmanes.caffeine.cache.Cache
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles
import java.time.Duration

@SpringBootTest
@ActiveProfiles("test")
class CacheConfigTest(
    private val cacheManager: CacheManager,
): StringSpec({
    val relayLocationDistinctDaysCache = cacheManager.getCache(CacheConfig.RELAY_LOCATION_DISTINCT_DAYS)
    val relayLocationsPerDayCache = cacheManager.getCache(CacheConfig.RELAY_LOCATIONS_PER_DAY)
    val reverseDnsLookupsCache = cacheManager.getCache(CacheConfig.REVERSE_DNS_LOOKUPS)

    @Suppress("UNCHECKED_CAST")
    fun nativeCache(name: String) = cacheManager.getCache(name)!!.nativeCache as Cache<Any, Any>

    beforeEach {
        relayLocationDistinctDaysCache?.clear()
        relayLocationsPerDayCache?.clear()
        reverseDnsLookupsCache?.clear()
    }

    "caches exists" {
        relayLocationDistinctDaysCache shouldNotBe null
        cacheManager.getCache(CacheConfig.RELAY_DAILY_COUNTS) shouldNotBe null
        relayLocationsPerDayCache shouldNotBe null
        reverseDnsLookupsCache shouldNotBe null
    }

    "unknown cache names are not created on demand" {
        cacheManager.getCache("UNKNOWN") shouldBe null
    }

    "caches are bounded" {
        mapOf(
            CacheConfig.RELAY_LOCATION_DISTINCT_DAYS to 1L,
            CacheConfig.RELAY_DAILY_COUNTS to 1L,
            CacheConfig.RELAY_LOCATIONS_PER_DAY to 40L,
            CacheConfig.COUNTRY_USAGE_PER_DAY to 400L,
            CacheConfig.REVERSE_DNS_LOOKUPS to 10_000L,
        ).forEach { (name, maximumSize) ->
            nativeCache(name).policy().eviction().get().maximum shouldBe maximumSize
        }
        val perDay = nativeCache(CacheConfig.RELAY_LOCATIONS_PER_DAY)
        repeat(100) { perDay.put("day-$it", it) }
        perDay.cleanUp()
        perDay.estimatedSize() shouldBeLessThanOrEqual 40L
    }

    "reverse DNS lookups expire after 6 hours" {
        nativeCache(CacheConfig.REVERSE_DNS_LOOKUPS).policy().expireAfterWrite().get().expiresAfter shouldBe
            Duration.ofHours(6)
    }
})
