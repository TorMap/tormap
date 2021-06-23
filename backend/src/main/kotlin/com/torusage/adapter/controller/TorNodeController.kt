package com.torusage.adapter.controller

import com.torusage.adapter.controller.exception.NodeNotFoundException
import com.torusage.database.entity.Relay
import com.torusage.database.repository.RelayRepository
import com.torusage.database.view.RelayView
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("node")
class TorNodeController(
    val relayRepository: RelayRepository,
) {

    @GetMapping("/relays")
    fun getRelays(): List<RelayView> {
        val relays = relayRepository.findAllByLatitudeNotNullAndLongitudeNotNull()
        return relays.map { RelayView(it) }
    }

    @GetMapping("/relay/{id}")
    fun getRelay(@PathVariable id: Long): Relay {
        return relayRepository.findById(id) ?: throw NodeNotFoundException()
    }
}
