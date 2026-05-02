package org.tormap.adapter.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.tormap.database.repository.RelayDetailsRepositoryImpl
import org.tormap.service.RelayDetailsQueryService
import javax.validation.constraints.Size

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
}
