package com.torusage.adapter.controller

import com.torusage.adapter.controller.exception.NodeNotFoundException
import com.torusage.adapter.controller.model.RelayView
import com.torusage.database.entity.recent.Relay
import com.torusage.database.repository.recent.RelayRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("recent")
class RecentDataController(
    val relayRepository: RelayRepository,
) {

    @GetMapping("/relay")
    fun getRelays(): List<RelayView> {
        val relays = relayRepository.findAllByLatitudeNotNullAndLongitudeNotNull()
        return relays.map { RelayView(it) }
    }

    @GetMapping("/relay/{id}")
    fun getRelay(@PathVariable id: Long): Relay {
        return relayRepository.findById(id) ?: throw NodeNotFoundException()
    }
}
