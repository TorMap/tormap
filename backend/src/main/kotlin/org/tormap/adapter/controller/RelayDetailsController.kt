package org.tormap.adapter.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.*
import org.tormap.database.repository.RelayDetailsRepositoryImpl
import org.tormap.service.RelayDetailsQueryService

@RestController
@RequestMapping("relay/details/")
class RelayDetailsController(
    val relayDetailsRepositoryImpl: RelayDetailsRepositoryImpl,
    val relayDetailsQueryService: RelayDetailsQueryService,
) {
    @Operation(summary = "Returns all relay details for a given relay.")
    @GetMapping("relay/{id}")
    fun getRelay(@PathVariable id: Long) = relayDetailsQueryService.getRelay(id)

    @Operation(summary = "Returns all relay details for a given family.")
    @GetMapping("family/{id}")
    fun getFamily(@PathVariable id: Long) = relayDetailsRepositoryImpl.findAllByFamilyId(id)

    @Operation(summary = "Returns all identifiers that are associated with a list of relay details IDs.")
    @PostMapping("relay/identifiers")
    fun getRelayIdentifiers(@RequestBody ids: List<Long>) = relayDetailsRepositoryImpl.findRelayIdentifiers(ids)

    @Operation(summary = "Returns family identifiers that are associated with a list of family IDs.")
    @PostMapping("family/identifiers")
    fun getFamilyIdentifiers(@RequestBody familyIds: List<Long>) =
        relayDetailsRepositoryImpl.findFamilyIdentifiers(familyIds)

    @Deprecated("Nickname is passed together with RelayLocationDto when quering a specific day")
    @Operation(summary = "Return the nicknames of the relays that are associated with a list of relay details IDs.")
    @PostMapping("relay/nicknames")
    fun getRelayNicknames(@RequestBody ids: List<Long>) = relayDetailsRepositoryImpl.findAllByIdIn(ids).map { it.nickname }
}
