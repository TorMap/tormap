package org.tormap.adapter.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.tormap.adapter.dto.AutonomousSystemDto
import org.tormap.config.CacheConfig
import org.tormap.database.repository.RelayDetailsRepositoryImpl
import org.tormap.service.RelayDetailsQueryService
import jakarta.validation.constraints.Size
import java.time.YearMonth
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("relay/details/")
@Validated
class RelayDetailsController(
    val relayDetailsRepositoryImpl: RelayDetailsRepositoryImpl,
    val relayDetailsQueryService: RelayDetailsQueryService,
) {
    companion object {
        internal const val MAXIMUM_EXPECTED_RELAYS_PER_DAY = 50000 // In 2026-05 we are around 10,000 relays per day
        internal const val MAXIMUM_EXPECTED_FAMILIES_PER_MONTH = 5000 // In 2026-05 we are around 500 unique families per month
    }

    @Operation(summary = "Returns all relay details for a given relay.")
    @GetMapping("relay/{id}")
    fun getRelay(@PathVariable id: Long) = relayDetailsQueryService.getRelay(id)

    @Operation(summary = "Returns all relay details for a given family.")
    @GetMapping("family/{id}")
    fun getFamily(@PathVariable id: Long) = relayDetailsRepositoryImpl.findAllByFamilyId(id)

    @Operation(summary = "Returns all identifiers that are associated with a list of relay details IDs.")
    @PostMapping("relay/identifiers")
    fun getRelayIdentifiers(@RequestBody @Size(min = 1, max = MAXIMUM_EXPECTED_RELAYS_PER_DAY) ids: List<Long>) = relayDetailsRepositoryImpl.findRelayIdentifiers(ids)

    @Operation(summary = "Returns family identifiers that are associated with a list of family IDs.")
    @PostMapping("family/identifiers")
    fun getFamilyIdentifiers(@RequestBody @Size(min = 1, max = MAXIMUM_EXPECTED_FAMILIES_PER_MONTH) familyIds: List<Long>) =
        relayDetailsRepositoryImpl.findFamilyIdentifiers(familyIds.distinct())

    // Months without autonomous systems are not cached, so requests for made-up months can not evict real ones
    @Cacheable(CacheConfig.AUTONOMOUS_SYSTEMS_PER_MONTH, key = "#month", unless = "#result.isEmpty()")
    @Operation(summary = "Returns the number and name of all autonomous systems that relays of a given month are located in.")
    @GetMapping("autonomous-systems/{month}")
    fun getAutonomousSystems(@PathVariable month: String): List<AutonomousSystemDto> {
        // The path variable is untrusted: a malformed month is a client error, not a server error
        val yearMonth = try {
            YearMonth.parse(month)
        } catch (_: DateTimeParseException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "The month must be like 2024-05.")
        }
        // The month is the cache key. Padded years like +002024-05 parse to the same month, but would fill the cache with copies.
        if (yearMonth.toString() != month) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "The month must be like 2024-05.")
        return relayDetailsRepositoryImpl.findAutonomousSystemsByMonth(yearMonth.toString())
    }
}
