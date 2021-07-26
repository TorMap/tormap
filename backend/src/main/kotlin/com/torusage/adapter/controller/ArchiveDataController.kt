package com.torusage.adapter.controller

import com.torusage.adapter.controller.exception.NodeNotFoundException
import com.torusage.adapter.controller.view.GeoRelayView
import com.torusage.database.entity.NodeDetails
import com.torusage.database.repository.GeoRelayRepositoryImpl
import com.torusage.database.repository.NodeDetailsRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("archive")
class ArchiveDataController(
    val geoRelayRepository: GeoRelayRepositoryImpl,
    val nodeDetailsRepository: NodeDetailsRepository,
) {
    @GetMapping("/geo/relay/days")
    fun getDaysForGeoRelays() = geoRelayRepository.findDistinctDays()

    @GetMapping("/geo/relay/day/{day}")
    fun getGeoRelaysByDay(@PathVariable day: String): List<GeoRelayView> {
        val relays = geoRelayRepository.findAllByDay(LocalDate.parse(day))
        return relays.map { GeoRelayView(it) }
    }

    @GetMapping("/node/details/{id}")
    fun getNodeDetails(@PathVariable id: Long): NodeDetails {
        val details = nodeDetailsRepository.findById(id)
        return if (details.isPresent) details.get() else throw NodeNotFoundException()
    }
}
