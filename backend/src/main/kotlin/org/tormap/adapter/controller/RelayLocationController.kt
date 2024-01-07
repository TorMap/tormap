package org.tormap.adapter.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.tormap.adapter.dto.RelayLocationDto
import org.tormap.config.CacheConfig
import org.tormap.database.repository.RelayLocationRepositoryImpl
import java.time.LocalDate

@RestController
@RequestMapping("relay/location/")
class RelayLocationController(
    val relayLocationRepositoryImpl: RelayLocationRepositoryImpl,
) {
    @Cacheable(CacheConfig.RELAY_LOCATION_DISTINCT_DAYS, key = "T(org.tormap.config.CacheConfig).RELAY_LOCATION_DISTINCT_DAYS_KEY")
    @Operation(summary = "Returns all distinct days for which relay locations are available.")
    @GetMapping("days")
    fun getDays(): Set<LocalDate> = relayLocationRepositoryImpl.findDistinctDays()

    @Cacheable(CacheConfig.RELAY_LOCATIONS_PER_DAY, key = "#day")
    @Operation(summary = "Returns all relay locations for a given day. In Swagger UI, the large result might freeze your browser tab!")
    @GetMapping("day/{day}")
    fun getDay(@PathVariable day: String): List<RelayLocationDto> =
        relayLocationRepositoryImpl.findAllUsingDay(LocalDate.parse(day))
}
