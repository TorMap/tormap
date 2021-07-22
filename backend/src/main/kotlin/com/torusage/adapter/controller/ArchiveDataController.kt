package com.torusage.adapter.controller

import com.torusage.adapter.controller.model.ArchiveGeoRelayView
import com.torusage.database.entity.archive.ArchiveNodeDetails
import com.torusage.database.repository.archive.ArchiveGeoRelayRepositoryImpl
import com.torusage.database.repository.archive.ArchiveNodeDetailsRepository
import com.torusage.logger
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

@RestController
@RequestMapping("archive")
class ArchiveDataController(
    val archiveGeoRelayRepository: ArchiveGeoRelayRepositoryImpl,
    val archiveNodeDetailsRepository: ArchiveNodeDetailsRepository,
) {
    val logger = logger()

    @GetMapping("/geo/relay/days")
    fun getDaysForGeoRelays() = archiveGeoRelayRepository.findDistinctDays()

    @GetMapping("/geo/relay/day/{day}")
    fun getGeoRelaysByDay(@PathVariable day: String): List<ArchiveGeoRelayView> {
        logger.info("Querying geo relays for day $day")
        val relays = archiveGeoRelayRepository.findAllByDay(LocalDate.parse(day))
        logger.info("Finished query for day $day")
        return relays.map { ArchiveGeoRelayView(it) }
    }

    @GetMapping("/node/details/{id}")
    fun getNodeDetails(@PathVariable id: Long): ArchiveNodeDetails {
        val details = archiveNodeDetailsRepository.findById(id)
        return if (details.isPresent) details.get() else throw ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "These node details do not exist!"
        )
    }
}
