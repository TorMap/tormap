package org.tormap.config

import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.jsr107.Eh107Configuration
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.cache.CacheManager
import javax.cache.Caching

@Configuration
@EnableCaching
class CacheConfig {
    companion object {
        const val RELAY_LOCATION_DISTINCT_DAYS = "RELAY_LOCATION_DISTINCT_DAYS"
        const val RELAY_LOCATION_DISTINCT_DAYS_KEY = "RELAY_LOCATION_DISTINCT_DAYS_KEY"
        const val RELAY_LOCATIONS_PER_DAY = "RELAY_LOCATIONS_OF_DAY"
    }

    @Bean
    fun getCacheManager(): CacheManager {
        val provider = Caching.getCachingProvider()
        val cacheManager = provider.cacheManager

        cacheManager.createCache(
            RELAY_LOCATION_DISTINCT_DAYS,
            Eh107Configuration.fromEhcacheCacheConfiguration(
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                    String::class.java, Set::class.java,
                    ResourcePoolsBuilder.heap(1)
                )
            )
        )

        cacheManager.createCache(
            RELAY_LOCATIONS_PER_DAY,
            Eh107Configuration.fromEhcacheCacheConfiguration(
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                    String::class.java, List::class.java,
                    ResourcePoolsBuilder.heap(40) // 1 entry ~= 2.5 MB of memory -> 40 entries ~= 100 MB of memory
                )
            )
        )
        return cacheManager
    }
}
