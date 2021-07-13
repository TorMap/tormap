package com.torusage.adapter.controller

import com.torusage.database.repository.archive.ArchiveGeoRelayRepositoryImpl
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("archive")
class ArchiveDataController(
    val archiveGeoRelayRepository: ArchiveGeoRelayRepositoryImpl,
) {

//    @GetMapping("/geo/relay/months")
//    fun getMonthsForGeoRelays() = archiveGeoRelayRepository.findDistinctMonths()

//    @GetMapping("/geo/relay/{month}")
//    fun getGeoRelays(@PathVariable month: String) =
//        archiveGeoRelayRepository.findAllByDay(month).map { ArchiveGeoRelayView(it) }
}
