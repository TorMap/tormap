package org.tormap.adapter.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.tormap.adapter.dto.RelayLocationDto
import org.tormap.config.CacheConfig
import org.tormap.database.repository.RelayLocationRepositoryImpl
import java.time.LocalDate
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("relay/location/")
class RelayLocationController(
    val relayLocationRepositoryImpl: RelayLocationRepositoryImpl,
) {
    @Cacheable(CacheConfig.RELAY_LOCATION_DISTINCT_DAYS, key = "T(org.tormap.config.CacheConfig).RELAY_LOCATION_DISTINCT_DAYS_KEY")
    @Operation(summary = "Returns all distinct days for which relay locations are available.")
    @GetMapping("days")
    fun getDays(): Set<LocalDate> = relayLocationRepositoryImpl.findDistinctDays()

    // Days without relays are not cached, so requests for made-up days can not evict the days that have data
    @Cacheable(CacheConfig.RELAY_LOCATIONS_PER_DAY, key = "#day", unless = "#result.isEmpty()")
    @Operation(summary = "Returns all relay locations for a given day. In Swagger UI, the large result might freeze your browser tab!")
    @GetMapping("day/{day}")
    fun getDay(@PathVariable day: String): List<RelayLocationDto> {
        // The path variable is untrusted: a malformed day is a client error, not a server error
        val date = try {
            LocalDate.parse(day)
        } catch (_: DateTimeParseException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "The day must be a date like 2024-05-01.")
        }
        // The day is the cache key. Padded years like +0002024-05-01 parse to the same date, but would fill the cache with copies.
        if (date.toString() != day) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "The day must be a date like 2024-05-01.")
        return relayLocationRepositoryImpl.findAllUsingDay(date)
    }
}
