package org.tormap.adapter.controller

import org.springframework.web.bind.annotation.*
import org.tormap.adapter.controller.exception.NodeNotFoundException
import org.tormap.adapter.controller.view.GeoRelayView
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
    @GetMapping("/geo/relay/days")
    fun getDaysForGeoRelays() = geoRelayRepository.findDistinctDays()

    @GetMapping("/geo/relay/day/{day}")
    fun getGeoRelaysByDay(@PathVariable day: String): List<GeoRelayView> =
        geoRelayRepository.findAllUsingDay(LocalDate.parse(day))

    @GetMapping("/node/details/{id}")
    fun getNodeDetails(@PathVariable id: Long): NodeDetails {
        val details = nodeDetailsRepositoryImpl.findById(id)
        return if (details.isPresent) details.get() else throw NodeNotFoundException()
    }

    @GetMapping("/node/family/{id}")
    fun getNodeFamily(@PathVariable id: Long) = nodeDetailsRepositoryImpl.findAllByFamilyId(id)

    @PostMapping("/node/identifiers")
    fun getNodeIdentifiers(@RequestBody ids: List<Long>) = nodeDetailsRepositoryImpl.findNodeIdentifiers(ids)
}
