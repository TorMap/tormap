package org.tormap.adapter.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.cache.annotation.Cacheable
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.tormap.adapter.dto.CountryUsageDto
import org.tormap.config.CacheConfig
import org.tormap.database.repository.CountryUsageRepository
import java.time.LocalDate

@RestController
@RequestMapping("relay/country-usage/")
class CountryUsageController(
    val countryUsageRepository: CountryUsageRepository,
) {
    @Cacheable(CacheConfig.COUNTRY_USAGE_PER_DAY, key = "#day")
    @Operation(
        summary = "Returns the estimated number of Tor relay users per country for a given day. " +
                "The list is empty for days without estimates (they exist from 2011-03 on). Source: Tor Metrics."
    )
    @GetMapping("day/{day}")
    fun getDay(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) day: LocalDate): List<CountryUsageDto> =
        countryUsageRepository.findAllByDay(day)
}
