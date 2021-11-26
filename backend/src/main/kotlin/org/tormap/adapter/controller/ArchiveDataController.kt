package org.tormap.adapter.controller

import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.*
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
    val geoRelayRepository: RelayLocationRepositoryImpl,
    val nodeDetailsRepositoryImpl: RelayDetailsRepositoryImpl,
) {
    @Cacheable("geo-relay-days")
    @GetMapping("/geo/relay/days")
    fun getDaysForGeoRelays() = geoRelayRepository.findDistinctDays()

    @Cacheable("geo-relay-day", key = "#day")
    @GetMapping("/geo/relay/day/{day}")
    fun getGeoRelaysByDay(@PathVariable day: String): List<RelayLocationDto> =
        geoRelayRepository.findAllUsingDay(LocalDate.parse(day))

    @GetMapping("/node/details/{id}")
    fun getNodeDetails(@PathVariable id: Long): RelayDetails {
        val details = nodeDetailsRepositoryImpl.findById(id)
        return if (details.isPresent) details.get() else throw RelayNotFoundException()
    }

    @GetMapping("/node/family/{familyId}")
    fun getNodesOfFamily(@PathVariable familyId: Long) = nodeDetailsRepositoryImpl.findAllByFamilyId(familyId)

    @PostMapping("/node/identifiers")
    fun getNodeIdentifiers(@RequestBody ids: List<Long>) = nodeDetailsRepositoryImpl.findRelayIdentifiers(ids)

    @PostMapping("/node/family/identifiers")
    fun getNodeFamilyIdentifiers(@RequestBody familyIds: List<Long>) =
        nodeDetailsRepositoryImpl.findFamilyIdentifiers(familyIds)
}
