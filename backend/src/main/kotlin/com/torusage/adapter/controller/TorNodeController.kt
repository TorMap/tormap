package com.torusage.adapter.controller

import com.torusage.database.repository.RelayRepository
import com.torusage.database.view.RelayView
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("node")
class TorNodeController(
    val relayRepository: RelayRepository,
) {

    @GetMapping("/relays")
    fun getRelays(): List<RelayView> {
        val relays = relayRepository.findAll()
        return relays.filter { it.longitude != null && it.latitude != null }.map { RelayView(it) }
    }
}
