package org.tormap.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig {
    companion object {
        const val RELAY_LOCATION_DISTINCT_DAYS = "RELAY_LOCATION_DISTINCT_DAYS"
        const val RELAY_LOCATION_DISTINCT_DAYS_KEY = "RELAY_LOCATION_DISTINCT_DAYS_KEY"
        const val RELAY_DAILY_COUNTS = "RELAY_DAILY_COUNTS"
        const val RELAY_DAILY_COUNTS_KEY = "RELAY_DAILY_COUNTS_KEY"
        const val RELAY_LOCATIONS_PER_DAY = "RELAY_LOCATIONS_OF_DAY"
        const val REVERSE_DNS_LOOKUPS = "REVERSE_DNS_LOOKUPS"
    }

    @Bean
    fun cacheManager(): CacheManager = CaffeineCacheManager().apply {
        // Only the caches registered below exist. Unknown names fail instead of creating unbounded caches.
        setCacheNames(emptyList())
        registerCustomCache(
            RELAY_LOCATION_DISTINCT_DAYS,
            // 1 entry ~= 250–300 KB for ~6,700 days (2007-10 to 2026-05)
            Caffeine.newBuilder().maximumSize(1).recordStats().build()
        )
        registerCustomCache(
            RELAY_DAILY_COUNTS,
            // 1 entry ~= 400 KB for ~6,700 days (4 numbers per day)
            Caffeine.newBuilder().maximumSize(1).recordStats().build()
        )
        registerCustomCache(
            RELAY_LOCATIONS_PER_DAY,
            // 1 entry ~= 2.5 MB of memory -> 40 entries ~= 100 MB of memory
            Caffeine.newBuilder().maximumSize(40).recordStats().build()
        )
        registerCustomCache(
            REVERSE_DNS_LOOKUPS,
            Caffeine.newBuilder()
                // 1 entry ~= 0.5 KB of memory -> 10,000 entries ~= 5 MB of memory
                .maximumSize(10_000)
                // 6h TTL balances DNS freshness for relay monitoring with cache efficiency
                .expireAfterWrite(Duration.ofHours(6))
                .recordStats()
                .build()
        )
    }
}
