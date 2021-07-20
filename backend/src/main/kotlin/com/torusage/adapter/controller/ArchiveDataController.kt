package com.torusage.adapter.controller

import com.torusage.adapter.controller.model.ArchiveGeoRelayView
import com.torusage.database.repository.archive.ArchiveGeoRelayRepositoryImpl
import com.torusage.database.repository.archive.ProcessedDescriptorRepositoryImpl
import com.torusage.logger
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
    val logger = logger()

    @GetMapping("/geo/relay/days")
    fun getDaysForGeoRelays() = processedDescriptorRepository.findDistinctDays()

    @GetMapping("/geo/relay/day/{day}")
    fun getGeoRelaysByDay(@PathVariable day: String): List<ArchiveGeoRelayView> {
        logger.info("Querying geo relays for day $day")
        val relays = archiveGeoRelayRepository.findAllByDay(LocalDate.parse(day))
        logger.info("Finished query for day $day")
        return relays.map { ArchiveGeoRelayView(it) }
    }
}
