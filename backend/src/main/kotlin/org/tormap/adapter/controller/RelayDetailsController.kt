package org.tormap.adapter.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.*
import org.tormap.adapter.controller.exception.RelayNotFoundException
import org.tormap.database.entity.RelayDetails
import org.tormap.database.repository.RelayDetailsRepositoryImpl

@RestController
@RequestMapping("relay/details/")
class RelayDetailsController(
    val relayDetailsRepositoryImpl: RelayDetailsRepositoryImpl,
) {
    @Operation(summary = "Returns all relay details for a given relay.")
    @GetMapping("relay/{id}")
    fun getRelay(@PathVariable id: Long): RelayDetails {
        val details = relayDetailsRepositoryImpl.findById(id)
        return if (details.isPresent) details.get() else throw RelayNotFoundException()
    }

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
}
