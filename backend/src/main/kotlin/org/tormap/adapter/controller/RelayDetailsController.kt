package org.tormap.adapter.controller

import org.springframework.web.bind.annotation.*
import org.tormap.adapter.controller.exception.RelayNotFoundException
import org.tormap.database.entity.RelayDetails
import org.tormap.database.repository.RelayDetailsRepositoryImpl

@RestController
@RequestMapping("relay/details/")
class RelayDetailsController(
    val relayDetailsRepositoryImpl: RelayDetailsRepositoryImpl,
) {
    @GetMapping("relay/{id}")
    fun getRelay(@PathVariable id: Long): RelayDetails {
        val details = relayDetailsRepositoryImpl.findById(id)
        return if (details.isPresent) details.get() else throw RelayNotFoundException()
    }

    @GetMapping("family/{id}")
    fun getFamily(@PathVariable id: Long) = relayDetailsRepositoryImpl.findAllByFamilyId(id)

    @PostMapping("relay/identifiers")
    fun getRelayIdentifiers(@RequestBody ids: List<Long>) = relayDetailsRepositoryImpl.findRelayIdentifiers(ids)

    @PostMapping("family/identifiers")
    fun getFamilyIdentifiers(@RequestBody familyIds: List<Long>) =
        relayDetailsRepositoryImpl.findFamilyIdentifiers(familyIds)
}
