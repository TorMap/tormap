package org.tormap.service

import org.springframework.cache.CacheManager
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.tormap.config.CacheConfig
import org.tormap.database.repository.RelayLocationRepositoryImpl
import org.tormap.util.logger
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock


@Service
class CacheService(
    private val cacheManager: CacheManager,
    private val relayLocationRepositoryImpl: RelayLocationRepositoryImpl,
) {
    private val logger = logger()
    private val lockRelayLocationDistinctDays: Lock = ReentrantLock()
    private val lockRelayLocationsPerDay: Lock = ReentrantLock()

    @Async
    fun cacheRelayLocationDistinctDays() {
        if (lockRelayLocationDistinctDays.tryLock()) {
            try {
                logger.info("Caching distinct relay location days")
                cacheManager.getCache(CacheConfig.RELAY_LOCATION_DISTINCT_DAYS)?.put(
                    CacheConfig.RELAY_LOCATION_DISTINCT_DAYS_KEY,
                    relayLocationRepositoryImpl.findDistinctDays()
                )
            } finally {
                lockRelayLocationDistinctDays.unlock()
            }
        } else {
            logger.debug("Cache update of relay location distinct days already in progress. Waiting 1 second...")
            Thread.sleep(1000)
            cacheRelayLocationDistinctDays()
        }
    }

    @Async
    fun evictRelayLocationsPerDay(months: Set<String>) {
        logger.info("Evicting cache of relay locations per day for months: ${months.joinToString(", ")}")
        months.forEach { month ->
            val yearMonth = YearMonth.parse(month)
            yearMonth.atDay(1).datesUntil(yearMonth.plusMonths(1).atDay(1)).forEach {
                cacheManager.getCache(CacheConfig.RELAY_LOCATIONS_PER_DAY)?.evict(it.toString())
            }
        }
    }

    @Async
    fun cacheRelayLocationsPerDay(months: Set<String>) {
        if (lockRelayLocationsPerDay.tryLock()) {
            try {
                logger.info("Caching relay locations for each day of months: ${months.joinToString(", ")}")
                months.forEach { month ->
                    val yearMonth = YearMonth.parse(month)
                    yearMonth.atDay(1).datesUntil(yearMonth.plusMonths(1).atDay(1)).forEach {
                        val day = it.toString()
                        cacheManager.getCache(CacheConfig.RELAY_LOCATIONS_PER_DAY)?.put(
                            day,
                            relayLocationRepositoryImpl.findAllUsingDay(LocalDate.parse(day))
                        )
                    }
                }
            } finally {
                lockRelayLocationsPerDay.unlock()
            }
        } else {
            logger.debug("Cache update of relay location per day already in progress. Waiting 1 second...")
            Thread.sleep(1000)
            cacheRelayLocationsPerDay(months)
        }
    }
}
