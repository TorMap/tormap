package org.tormap.adapter.controller

import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.tormap.CacheName
import org.tormap.adapter.dto.RelayLocationDto
import org.tormap.database.repository.RelayLocationRepositoryImpl
import java.time.LocalDate

@RestController
@RequestMapping("relay/location/")
class RelayLocationController(
    val relayLocationRepositoryImpl: RelayLocationRepositoryImpl,
) {
    @Cacheable(CacheName.RELAY_LOCATION_DAYS)
    @GetMapping("days")
    fun getDaysForGeoRelays() = relayLocationRepositoryImpl.findDistinctDays()

    @Cacheable(CacheName.RELAY_LOCATION_DAY, key = "#day")
    @GetMapping("day/{day}")
    fun getGeoRelaysByDay(@PathVariable day: String): List<RelayLocationDto> =
        relayLocationRepositoryImpl.findAllUsingDay(LocalDate.parse(day))
}
