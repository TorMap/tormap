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
        const val COUNTRY_USAGE_PER_DAY = "COUNTRY_USAGE_PER_DAY"
        const val REVERSE_DNS_LOOKUPS = "REVERSE_DNS_LOOKUPS"
        const val LITE_MAP_IMAGES = "LITE_MAP_IMAGES"
        const val AUTONOMOUS_SYSTEMS_PER_MONTH = "AUTONOMOUS_SYSTEMS_PER_MONTH"
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
            COUNTRY_USAGE_PER_DAY,
            // 1 entry ~= 15 KB for ~250 countries -> 400 entries ~= 6 MB of memory
            Caffeine.newBuilder().maximumSize(400).recordStats().build()
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
        registerCustomCache(
            AUTONOMOUS_SYSTEMS_PER_MONTH,
            Caffeine.newBuilder()
                // 1 entry ~= 100 KB for ~2,500 autonomous systems -> 24 entries ~= 2.4 MB of memory
                .maximumSize(24)
                // New relays get their autonomous system in the background, so the list is refreshed after a while
                .expireAfterWrite(Duration.ofHours(1))
                .recordStats()
                .build()
        )
        registerCustomCache(
            LITE_MAP_IMAGES,
            Caffeine.newBuilder()
                // 1 entry ~= 50–150 KB of PNG -> 100 entries ~= 15 MB of memory
                .maximumSize(100)
                // Days are reprocessed when new descriptors arrive, so images are redrawn after a while
                .expireAfterWrite(Duration.ofHours(1))
                .recordStats()
                .build()
        )
    }
}
