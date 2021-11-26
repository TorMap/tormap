package org.tormap.adapter.controller

import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.*
import org.tormap.CacheName
import org.tormap.adapter.controller.exception.RelayNotFoundException
import org.tormap.adapter.dto.RelayLocationDto
import org.tormap.database.entity.RelayDetails
import org.tormap.database.repository.RelayLocationRepositoryImpl
import org.tormap.database.repository.RelayDetailsRepositoryImpl
import java.time.LocalDate

// TODO Remove after backend deploy & frontend migration
@RestController
@RequestMapping("archive")
class ArchiveDataController(
    val relayLocationRepositoryImpl: RelayLocationRepositoryImpl,
    val relayDetailsRepositoryImpl: RelayDetailsRepositoryImpl,
) {
    @Cacheable(CacheName.RELAY_LOCATION_DAYS)
    @GetMapping("/geo/relay/days")
    fun getDaysForGeoRelays() = relayLocationRepositoryImpl.findDistinctDays()

    @Cacheable(CacheName.RELAY_LOCATION_DAY, key = "#day")
    @GetMapping("/geo/relay/day/{day}")
    fun getGeoRelaysByDay(@PathVariable day: String): List<RelayLocationDto> =
        relayLocationRepositoryImpl.findAllUsingDay(LocalDate.parse(day))

    @GetMapping("/node/details/{id}")
    fun getNodeDetails(@PathVariable id: Long): RelayDetails {
        val details = relayDetailsRepositoryImpl.findById(id)
        return if (details.isPresent) details.get() else throw RelayNotFoundException()
    }

    @GetMapping("/node/family/{familyId}")
    fun getNodesOfFamily(@PathVariable familyId: Long) = relayDetailsRepositoryImpl.findAllByFamilyId(familyId)

    @PostMapping("/node/identifiers")
    fun getNodeIdentifiers(@RequestBody ids: List<Long>) = relayDetailsRepositoryImpl.findRelayIdentifiers(ids)

    @PostMapping("/node/family/identifiers")
    fun getNodeFamilyIdentifiers(@RequestBody familyIds: List<Long>) =
        relayDetailsRepositoryImpl.findFamilyIdentifiers(familyIds)
}
