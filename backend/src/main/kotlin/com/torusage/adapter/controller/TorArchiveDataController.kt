package com.torusage.adapter.controller

import com.torusage.adapter.controller.model.ArchiveGeoRelaysResponse
import com.torusage.database.repository.archive.ArchiveGeoRelayRepositoryImpl
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("archive")
class TorArchiveDataController(
    val geoNodeRepository: ArchiveGeoRelayRepositoryImpl,
) {

    @GetMapping("/geo/relays/{month}")
    fun getGeoRelays(@PathVariable month: String?): ArchiveGeoRelaysResponse {
        val months = geoNodeRepository.findDistinctMonths()
        val requestedMonth = month ?: months.last()
        return ArchiveGeoRelaysResponse(
            months,
            requestedMonth,
            geoNodeRepository.findAllById_SeenInMonth(requestedMonth),
        )
    }
}
