package org.tormap.adapter.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.tormap.adapter.dto.RelayLocationDto
import org.tormap.database.repository.RelayLocationRepository
import java.time.LocalDate

@RestController
@RequestMapping("relay/location/")
class RelayLocationController(val relayLocationRepository: RelayLocationRepository) {
    @GetMapping("days")
    fun getDays() = relayLocationRepository.findDistinctDays()

    @GetMapping("day/{day}")
    fun getDay(@PathVariable day: LocalDate): List<RelayLocationDto> = relayLocationRepository.findAllUsingDay(day)
}
