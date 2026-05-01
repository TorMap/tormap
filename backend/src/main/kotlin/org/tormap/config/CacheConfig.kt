package org.tormap.config

import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.ExpiryPolicyBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.jsr107.Eh107Configuration
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.tormap.service.ReverseDnsLookupResult
import java.time.Duration
import javax.cache.CacheManager
import javax.cache.Caching

@Configuration
@EnableCaching
class CacheConfig {
    companion object {
        const val RELAY_LOCATION_DISTINCT_DAYS = "RELAY_LOCATION_DISTINCT_DAYS"
        const val RELAY_LOCATION_DISTINCT_DAYS_KEY = "RELAY_LOCATION_DISTINCT_DAYS_KEY"
        const val RELAY_LOCATIONS_PER_DAY = "RELAY_LOCATIONS_OF_DAY"
        const val REVERSE_DNS_LOOKUPS = "REVERSE_DNS_LOOKUPS"
    }

    @Bean
    fun getCacheManager(): CacheManager {
        val provider = Caching.getCachingProvider()
        val cacheManager = provider.cacheManager

        if (!cacheManager.cacheNames.contains(RELAY_LOCATION_DISTINCT_DAYS)) {
            cacheManager.createCache(
                RELAY_LOCATION_DISTINCT_DAYS,
                Eh107Configuration.fromEhcacheCacheConfiguration(
                    CacheConfigurationBuilder.newCacheConfigurationBuilder(
                        String::class.java, Set::class.java,
                        ResourcePoolsBuilder.heap(1)  // 1 entry ~= 250–300 KB for ~6,700 days (2007-10 to 2026-05)
                    )
                )
            )
        }
        if (!cacheManager.cacheNames.contains(RELAY_LOCATIONS_PER_DAY)) {
            cacheManager.createCache(
                RELAY_LOCATIONS_PER_DAY,
                Eh107Configuration.fromEhcacheCacheConfiguration(
                    CacheConfigurationBuilder.newCacheConfigurationBuilder(
                        String::class.java, List::class.java,
                        ResourcePoolsBuilder.heap(40) // 1 entry ~= 2.5 MB of memory -> 40 entries ~= 100 MB of memory
                    )
                )
            )
        }
        if (!cacheManager.cacheNames.contains(REVERSE_DNS_LOOKUPS)) {
            cacheManager.createCache(
                REVERSE_DNS_LOOKUPS,
                Eh107Configuration.fromEhcacheCacheConfiguration(
                    CacheConfigurationBuilder.newCacheConfigurationBuilder(
                        String::class.java,
                        ReverseDnsLookupResult::class.java,
                        ResourcePoolsBuilder.heap(10000) // 1 entry ~= 0.5 KB of memory -> 10,000 entries ~= 5 MB of memory
                    )
                        // 6h TTL balances DNS freshness for relay monitoring with cache efficiency
                        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofHours(6)))
                )
            )
        }
        return cacheManager
    }
}
