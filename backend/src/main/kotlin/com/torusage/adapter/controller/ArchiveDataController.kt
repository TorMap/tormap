package com.torusage.adapter.controller

import com.torusage.adapter.controller.model.ArchiveGeoRelayView
import com.torusage.database.repository.archive.ArchiveGeoRelayRepositoryImpl
import com.torusage.database.repository.archive.ProcessedDescriptorRepositoryImpl
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("archive")
class ArchiveDataController(
    val archiveGeoRelayRepository: ArchiveGeoRelayRepositoryImpl,
    val processedDescriptorRepository: ProcessedDescriptorRepositoryImpl,
) {
    @GetMapping("/geo/relay/days")
    fun getDaysForGeoRelays() = processedDescriptorRepository.findDistinctDays()

    @GetMapping("/geo/relay/day/{day}")
    fun getGeoRelaysByDay(@PathVariable day: String) =
        archiveGeoRelayRepository.findAllByDay(LocalDate.parse(day)).map { ArchiveGeoRelayView(it) }
}
