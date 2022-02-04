package org.tormap.adapter.controller

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.scheduling.annotation.Async
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.tormap.adapter.dto.RelayLocationDto
import org.tormap.database.repository.RelayLocationRepositoryImpl
import java.time.LocalDate
import java.time.YearMonth

@RestController
@RequestMapping("relay/location/")
class RelayLocationController(
    val relayLocationRepositoryImpl: RelayLocationRepositoryImpl,
    val cacheManager: CacheManager,
) {
    object CacheName {
        const val RELAY_LOCATION_DAYS = "RELAY_LOCATION_DAYS"
        const val RELAY_LOCATION_DAY = "RELAY_LOCATION_DAY"
    }

    @Cacheable(CacheName.RELAY_LOCATION_DAYS)
    @GetMapping("days")
    fun getDays() = relayLocationRepositoryImpl.findDistinctDays()

    @Cacheable(CacheName.RELAY_LOCATION_DAY, key = "#day")
    @GetMapping("day/{day}")
    fun getDay(@PathVariable day: String): List<RelayLocationDto> =
        relayLocationRepositoryImpl.findAllUsingDay(LocalDate.parse(day))

    /**
     * Update the cache for available relay location days and the given [day]
     */
    @Async
    fun cacheNewDay(day: String) {
        cacheManager.getCache(CacheName.RELAY_LOCATION_DAYS)?.invalidate()
        getDays()
        cacheManager.getCache(CacheName.RELAY_LOCATION_DAY)?.put(
            day,
            relayLocationRepositoryImpl.findAllUsingDay(LocalDate.parse(day))
        )
    }

    /**
     * Updates the relay location cache for all days of a given [month]
     */
    @Async
    fun cacheDaysOfMonth(month: String) {
        val yearMonth = YearMonth.parse(month)
        yearMonth.atDay(1).datesUntil(yearMonth.plusMonths(1).atDay(1)).forEach {
            val day = it.toString()
            cacheManager.getCache(CacheName.RELAY_LOCATION_DAY)?.put(
                day,
                relayLocationRepositoryImpl.findAllUsingDay(LocalDate.parse(day))
            )
        }
    }
}
