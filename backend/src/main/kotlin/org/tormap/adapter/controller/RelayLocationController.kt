package org.tormap.adapter.controller

import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.tormap.adapter.dto.RelayLocationDto
import org.tormap.database.repository.RelayLocationRepositoryImpl
import java.time.LocalDate

@RestController
@RequestMapping("relay/location/")
class RelayLocationController(
    val relayLocationRepositoryImpl: RelayLocationRepositoryImpl,
) {
    object CacheName {
        const val RELAY_LOCATION_DAYS = "RELAY_LOCATION_DAYS"
    }

    @Cacheable(CacheName.RELAY_LOCATION_DAYS)
    @GetMapping("days")
    fun getDays() = relayLocationRepositoryImpl.findDistinctDays()

    @GetMapping("day/{day}")
    fun getDay(@PathVariable day: String): List<RelayLocationDto> =
        relayLocationRepositoryImpl.findAllUsingDay(LocalDate.parse(day))
}
