package org.tormap.adapter.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.tormap.adapter.controller.exception.RelayNotFoundException
import org.tormap.adapter.dto.AutonomousSystemDto
import org.tormap.adapter.dto.CountryUsageDto
import org.tormap.adapter.dto.DailyRelayCountDto
import org.tormap.adapter.dto.RelayDetailsDto
import org.tormap.adapter.dto.RelayIdentifiersDto
import org.tormap.adapter.dto.v1.RelayV1Dto
import org.tormap.config.CacheConfig
import org.tormap.database.repository.RelayDetailsRepositoryImpl
import org.tormap.database.repository.RelayLocationRepositoryImpl
import org.tormap.service.RelayDetailsQueryService
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

/**
 * The public, versioned API for the TorMap CLI and other clients. GET only.
 *
 * The paths, parameters and response fields are a contract, recorded in api/openapi-v1.json: within v1 only add
 * endpoints or fields. Renaming or removing anything needs a /v2/ path. Filters (country, flag, AS, nickname) run in
 * the client over a cached day, so request parameters can not multiply the server-side cache entries.
 *
 * Most endpoints delegate to the frontend controllers, so they share their validation and caches.
 */
@RestController
@RequestMapping(ApiV1Controller.PATH)
@Tag(name = "API v1", description = "Stable public API, used by the TorMap CLI")
class ApiV1Controller(
    private val relayLocationController: RelayLocationController,
    private val relayStatisticsController: RelayStatisticsController,
    private val relayDetailsController: RelayDetailsController,
    private val countryUsageController: CountryUsageController,
    private val relayLocationRepositoryImpl: RelayLocationRepositoryImpl,
    private val relayDetailsRepositoryImpl: RelayDetailsRepositoryImpl,
    private val relayDetailsQueryService: RelayDetailsQueryService,
) {
    companion object {
        const val PATH = "/v1/"
        private val FINGERPRINT = Regex("^[0-9A-Fa-f]{40}$")
    }

    @Operation(summary = "Returns all days with relay data, oldest first.")
    @GetMapping("days")
    fun getDays(): List<LocalDate> = relayLocationController.getDays().sorted()

    // Days without relays are not cached, so requests for made-up days can not evict the days that have data
    @Cacheable(CacheConfig.API_V1_RELAYS_PER_DAY, key = "#day", unless = "#result.isEmpty()")
    @Operation(summary = "Returns all relays of a day with fingerprint, nickname, country, location, flags, consensus weight, autonomous system and family.")
    @GetMapping("days/{day}/relays")
    fun getRelaysOfDay(@PathVariable day: String): List<RelayV1Dto> =
        relayLocationRepositoryImpl.findAllV1UsingDay(parseDay(day))

    @Operation(summary = "Returns the details of a relay by its details ID.")
    @GetMapping("relays/{id}")
    fun getRelay(@PathVariable id: Long): RelayDetailsDto = relayDetailsQueryService.getRelay(id)

    @Operation(summary = "Returns the details of a relay by its fingerprint, for the given month or the latest month with data.")
    @GetMapping("relays/by-fingerprint/{fingerprint}")
    fun getRelayByFingerprint(
        @PathVariable fingerprint: String,
        @RequestParam(required = false) month: String?,
    ): RelayDetailsDto {
        if (!FINGERPRINT.matches(fingerprint)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "The fingerprint must be 40 hexadecimal characters.")
        }
        val yearMonth = month?.let(::parseMonth)
            ?: relayLocationController.getDays().maxOrNull()?.let(YearMonth::from)
            ?: throw RelayNotFoundException()
        val details = relayDetailsRepositoryImpl.findByMonthAndFingerprint(yearMonth.toString(), fingerprint.uppercase())
            ?: throw RelayNotFoundException()
        return relayDetailsQueryService.getRelay(details.id!!)
    }

    @Operation(summary = "Returns the fingerprint, nickname and details ID of all relays in a family.")
    @GetMapping("families/{id}")
    fun getFamily(@PathVariable id: Long): List<RelayIdentifiersDto> =
        relayDetailsRepositoryImpl.findAllByFamilyId(id)
            .map { RelayIdentifiersDto(it.id!!, it.fingerprint, it.nickname) }
            .sortedBy { it.nickname.lowercase() }

    @Operation(summary = "Returns the number and name of all autonomous systems with relays in a month (like 2024-05).")
    @GetMapping("autonomous-systems/{month}")
    fun getAutonomousSystems(@PathVariable month: String): List<AutonomousSystemDto> =
        relayDetailsController.getAutonomousSystems(month)

    @Operation(summary = "Returns the number of relays (total, exit, guard, other) per day, optionally limited to a range of days.")
    @GetMapping("statistics/daily-counts")
    fun getDailyCounts(
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
    ): List<DailyRelayCountDto> {
        val fromDay = from?.let(::parseDay) ?: LocalDate.MIN
        val toDay = to?.let(::parseDay) ?: LocalDate.MAX
        if (fromDay > toDay) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must not be after 'to'.")
        return relayStatisticsController.getDailyCounts().filter { it.day in fromDay..toDay }
    }

    @Operation(summary = "Returns the estimated number of Tor users per country for a day. Source: Tor Metrics.")
    @GetMapping("country-usage/{day}")
    fun getCountryUsage(@PathVariable day: String): List<CountryUsageDto> =
        countryUsageController.getDay(parseDay(day))

    // Path and query values are untrusted: malformed values are client errors, and padded years like +0002024 are
    // rejected because they would parse to the same date but create more cache keys
    private fun parseDay(day: String): LocalDate {
        val date = try {
            LocalDate.parse(day)
        } catch (_: DateTimeParseException) {
            null
        }
        if (date == null || date.toString() != day) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "The day must be a date like 2024-05-01.")
        }
        return date
    }

    private fun parseMonth(month: String): YearMonth {
        val yearMonth = try {
            YearMonth.parse(month)
        } catch (_: DateTimeParseException) {
            null
        }
        if (yearMonth == null || yearMonth.toString() != month) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "The month must be like 2024-05.")
        }
        return yearMonth
    }
}
