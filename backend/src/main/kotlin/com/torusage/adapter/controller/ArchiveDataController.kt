package com.torusage.adapter.controller

import com.torusage.adapter.controller.model.ArchiveGeoRelayView
import com.torusage.database.repository.archive.ArchiveGeoRelayRepositoryImpl
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("archive")
class ArchiveDataController(
    val archiveGeoRelayRepository: ArchiveGeoRelayRepositoryImpl,
) {

    @GetMapping("/geo/relay/months")
    fun getMonthsForGeoRelays() = archiveGeoRelayRepository.findDistinctMonths()

    @GetMapping("/geo/relay/{month}")
    fun getGeoRelays(@PathVariable month: String) =
        archiveGeoRelayRepository.findAllById_SeenInMonth(month).map { ArchiveGeoRelayView(it) }
}
