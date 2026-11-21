package org.tormap.adapter.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
import org.tormap.adapter.dto.v1.FamilyV1Dto
import org.tormap.adapter.dto.v1.RelayDetailsV1Dto
import org.tormap.adapter.dto.v1.RelayIdentifiersV1Dto
import org.tormap.adapter.dto.v1.RelayV1Dto
import org.tormap.config.CacheConfig
import org.tormap.database.repository.RelayDetailsRepositoryImpl
import org.tormap.service.ApiV1RelaysOfDayService
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
    private val relayDetailsRepositoryImpl: RelayDetailsRepositoryImpl,
    private val relayDetailsQueryService: RelayDetailsQueryService,
    private val apiV1RelaysOfDayService: ApiV1RelaysOfDayService,
) {
    companion object {
        const val PATH = "/v1/"
        private val FINGERPRINT = Regex("^[0-9A-Fa-f]{40}$")
    }

    @Operation(summary = "Returns all days with relay data, oldest first.")
    @GetMapping("days")
    fun getDays(): List<LocalDate> = relayLocationController.getDays().sorted()

    // Answers If-None-Match with 304 Not Modified, so clients can revalidate a cached day without downloading it again
    @Operation(summary = "Returns all relays of a day with fingerprint, nickname, country, location, flags, consensus weight, autonomous system and family.")
    @GetMapping("days/{day}/relays")
    fun getRelaysOfDay(@PathVariable day: String): ResponseEntity<List<RelayV1Dto>> {
        val relaysOfDay = apiV1RelaysOfDayService.getRelaysOfDay(parseDay(day))
        return ResponseEntity.ok().eTag(relaysOfDay.eTag).body(relaysOfDay.relays)
    }

    // The fingerprint is the relay's identity in Tor and stays the same across months, unlike the internal details ID.
    // The lookup uses the unique (month, fingerprint) index, also when no month is given (see RelayDetailsIndexUsageTest).
    @Operation(summary = "Returns the details of a relay by its fingerprint, for the given month (like 2024-05) or the latest month with data.")
    @GetMapping("relays/{fingerprint}")
    fun getRelay(
        @PathVariable fingerprint: String,
        @RequestParam(required = false) month: String?,
    ): RelayDetailsV1Dto {
        if (!FINGERPRINT.matches(fingerprint)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "The fingerprint must be 40 hexadecimal characters.")
        }
        val yearMonth = month?.let(::parseMonth)
            ?: relayLocationController.getDays().maxOrNull()?.let(YearMonth::from)
            ?: throw RelayNotFoundException()
        val details = relayDetailsRepositoryImpl.findByMonthAndFingerprint(yearMonth.toString(), fingerprint.uppercase())
            ?: throw RelayNotFoundException()
        return RelayDetailsV1Dto(relayDetailsQueryService.getRelay(details.id!!))
    }

    // Months without families are not cached, so requests for made-up months can not evict real ones
    @Cacheable(CacheConfig.API_V1_FAMILIES_PER_MONTH, key = "#month", unless = "#result.isEmpty()")
    @Operation(summary = "Returns all families of a month (like 2024-05) with their members and autonomous systems.")
    @GetMapping("families")
    fun getFamilies(@RequestParam month: String): List<FamilyV1Dto> =
        relayDetailsRepositoryImpl.findFamilyMembersByMonth(parseMonth(month).toString())
            .groupBy { it.familyId }
            .map { (id, members) ->
                FamilyV1Dto(
                    id = id,
                    members = members
                        .map { RelayIdentifiersV1Dto(it.fingerprint, it.nickname) }
                        .sortedWith(compareBy({ it.nickname.lowercase() }, { it.fingerprint })),
                    autonomousSystems = members
                        .mapNotNull { member -> member.autonomousSystemNumber?.let { AutonomousSystemDto(it, member.autonomousSystemName) } }
                        .distinctBy { it.number }
                        .sortedBy { it.number },
                )
            }
            .sortedBy { it.id }

    @Operation(summary = "Returns the fingerprint and nickname of all relays in a family.")
    @GetMapping("families/{id}")
    fun getFamily(@PathVariable id: Long): List<RelayIdentifiersV1Dto> =
        relayDetailsRepositoryImpl.findAllByFamilyId(id)
            .map { RelayIdentifiersV1Dto(it.fingerprint, it.nickname) }
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
