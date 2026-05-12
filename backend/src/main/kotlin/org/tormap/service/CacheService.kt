package org.tormap.service

import org.springframework.cache.CacheManager
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.tormap.config.CacheConfig
import org.tormap.database.repository.RelayLocationRepositoryImpl
import org.tormap.util.logger
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.CompletableFuture


@Service
class CacheService(
    private val cacheManager: CacheManager,
    private val relayLocationRepositoryImpl: RelayLocationRepositoryImpl,
    private val coalesceService: CoalesceService,
) {
    private val logger = logger()

    fun cacheRelayLocationDistinctDays(): CompletableFuture<Void> {
        return coalesceService.submit("cacheRelayLocationDistinctDays") {
            logger.info("Caching distinct relay location days")
            cacheManager.getCache(CacheConfig.RELAY_LOCATION_DISTINCT_DAYS)?.put(
                CacheConfig.RELAY_LOCATION_DISTINCT_DAYS_KEY,
                relayLocationRepositoryImpl.findDistinctDays()
            )
        }
    }

    fun cacheRelayLocationsPerDay(months: Set<String>): CompletableFuture<Void> {
        logger.info("Caching relay locations for each day of months: {}", months.joinToString(", "))
        val futures = months.map { month ->
            coalesceService.submit("cacheRelayLocationsPerDay-$month") {
                val yearMonth = YearMonth.parse(month)
                yearMonth.atDay(1).datesUntil(yearMonth.plusMonths(1).atDay(1)).forEach {
                    val day = it.toString()
                    val relayLocations = relayLocationRepositoryImpl.findAllUsingDay(LocalDate.parse(day))
                    if (relayLocations.isNotEmpty()) {
                        cacheManager.getCache(CacheConfig.RELAY_LOCATIONS_PER_DAY)?.put(
                            day,
                            relayLocations
                        )
                    }
                }
            }
        }
        return CompletableFuture.allOf(*futures.toTypedArray())
    }

    @Async
    fun evictRelayLocationsPerDay(months: Set<String>): CompletableFuture<Void> {
        logger.info("Evicting cache of relay locations per day for months: ${months.joinToString(", ")}")
        months.forEach { month ->
            val yearMonth = YearMonth.parse(month)
            yearMonth.atDay(1).datesUntil(yearMonth.plusMonths(1).atDay(1)).forEach {
                cacheManager.getCache(CacheConfig.RELAY_LOCATIONS_PER_DAY)?.evict(it.toString())
            }
        }
        return CompletableFuture.completedFuture(null)
    }
}
