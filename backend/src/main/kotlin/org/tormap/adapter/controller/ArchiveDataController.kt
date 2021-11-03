package org.tormap.adapter.controller

import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.*
import org.tormap.adapter.controller.exception.NodeNotFoundException
import org.tormap.adapter.dto.GeoRelayDto
import org.tormap.database.entity.NodeDetails
import org.tormap.database.repository.GeoRelayRepositoryImpl
import org.tormap.database.repository.NodeDetailsRepositoryImpl
import java.time.LocalDate

@RestController
@RequestMapping("archive")
class ArchiveDataController(
    val geoRelayRepository: GeoRelayRepositoryImpl,
    val nodeDetailsRepositoryImpl: NodeDetailsRepositoryImpl,
) {
    @Cacheable("geo-relay-days")
    @GetMapping("/geo/relay/days")
    fun getDaysForGeoRelays() = geoRelayRepository.findDistinctDays()

    @Cacheable("geo-relay-day", key = "#day")
    @GetMapping("/geo/relay/day/{day}")
    fun getGeoRelaysByDay(@PathVariable day: String): List<GeoRelayDto> =
        geoRelayRepository.findAllUsingDay(LocalDate.parse(day))

    @GetMapping("/node/details/{id}")
    fun getNodeDetails(@PathVariable id: Long): NodeDetails {
        val details = nodeDetailsRepositoryImpl.findById(id)
        return if (details.isPresent) details.get() else throw NodeNotFoundException()
    }

    @GetMapping("/node/family/{familyId}")
    fun getNodesOfFamily(@PathVariable familyId: Long) = nodeDetailsRepositoryImpl.findAllByFamilyId(familyId)

    @PostMapping("/node/identifiers")
    fun getNodeIdentifiers(@RequestBody ids: List<Long>) = nodeDetailsRepositoryImpl.findNodeIdentifiers(ids)

    @PostMapping("/node/family/identifiers")
    fun getNodeFamilyIdentifiers(@RequestBody familyIds: List<Long>) =
        nodeDetailsRepositoryImpl.findFamilyIdentifiers(familyIds)
}
