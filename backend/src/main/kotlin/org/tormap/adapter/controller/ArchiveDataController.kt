package org.tormap.adapter.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.tormap.adapter.controller.exception.NodeNotFoundException
import org.tormap.adapter.controller.view.FamilyView
import org.tormap.adapter.controller.view.GeoRelayView
import org.tormap.calculateIPv4NumberRepresentation
import org.tormap.database.entity.IpLookupAs
import org.tormap.database.entity.NodeDetails
import org.tormap.database.repository.GeoRelayRepositoryImpl
import org.tormap.database.repository.IpLookupAsRepositoryImpl
import org.tormap.database.repository.NodeDetailsRepository
import java.time.LocalDate

@RestController
@RequestMapping("archive")
class ArchiveDataController(
    val geoRelayRepository: GeoRelayRepositoryImpl,
    val nodeDetailsRepository: NodeDetailsRepository,
    val ipLookupAsRepositoryImpl: IpLookupAsRepositoryImpl,
) {
    @GetMapping("/geo/relay/days")
    fun getDaysForGeoRelays() = geoRelayRepository.findDistinctDays()

    @GetMapping("/geo/relay/day/{day}")
    fun getGeoRelaysByDay(@PathVariable day: String): List<GeoRelayView> =
        geoRelayRepository.findAllUsingDay(LocalDate.parse(day))

    @GetMapping("/node/details/{id}")
    fun getNodeDetails(@PathVariable id: Long): NodeDetails {
        val details = nodeDetailsRepository.findById(id)
        return if (details.isPresent) details.get() else throw NodeNotFoundException()
    }

    @GetMapping("/node/family/{id}")
    fun getNodeFamily(@PathVariable id: Long): FamilyView {
        val familyMembers = nodeDetailsRepository.findAllByFamilyId(id)
        if (familyMembers.isEmpty()) throw NodeNotFoundException()

        val autonomousSystems = mutableSetOf<IpLookupAs>()
        familyMembers.forEach {
            val autonomousSystem = ipLookupAsRepositoryImpl.findUsingIPv4(
                calculateIPv4NumberRepresentation(it.address!!)
            )
            if (autonomousSystem != null) {
                autonomousSystems.add(autonomousSystem)
            }
        }
        return FamilyView(
            familyMembers,
            autonomousSystems,
        )
    }
}
