package com.torusage.adapter.controller

import com.torusage.adapter.controller.exception.NodeNotFoundException
import com.torusage.adapter.controller.model.GeoNodeResponse
import com.torusage.adapter.controller.model.RelayResponse
import com.torusage.database.entity.Relay
import com.torusage.database.repository.GeoNodeRepositoryImpl
import com.torusage.database.repository.RelayRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("node")
class TorNodeController(
    val relayRepository: RelayRepository,
    val geoNodeRepository: GeoNodeRepositoryImpl,
) {

    @GetMapping("/relays")
    fun getRelays(): List<RelayResponse> {
        val relays = relayRepository.findAllByLatitudeNotNullAndLongitudeNotNull()
        return relays.map { RelayResponse(it) }
    }

    @GetMapping("/relay/{id}")
    fun getRelay(@PathVariable id: Long): Relay {
        return relayRepository.findById(id) ?: throw NodeNotFoundException()
    }

    @GetMapping("/geo/{month}")
    fun getGeoNodes(@PathVariable month: String?): GeoNodeResponse {
        val months = geoNodeRepository.findDistinctMonths()
        val requestedMonth = month ?: months.last()
        return GeoNodeResponse(
            availableMonths = months,
            requestedMonth = requestedMonth,
            geoNodes = geoNodeRepository.findAllById_SeenInMonth(requestedMonth),
        )
    }
}
