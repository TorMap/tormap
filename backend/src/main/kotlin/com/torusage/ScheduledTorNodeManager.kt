package com.torusage

import com.torusage.adapter.OnionooApiClient
import com.torusage.common.logger
import com.torusage.database.RelaySummaryRepositories
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


@Component
class ScheduledTorNodeManager(
    val relaySummaryRepositories: RelaySummaryRepositories,
    val onionooApiClient: OnionooApiClient,
) {
    /**
     * Fetches Tor node data every 30 seconds and saves corresponding entities in DB.
     */
    @Scheduled(fixedRate = 30000)
    fun fetchAndStoreTorNodes() {
        logger().info("Fetching Tor node data with OnionooApiClient.")
        val relays = onionooApiClient.getTorNodeSummary(limitCount = 10).relays
        relays.forEach { relaySummaryRepositories.save(it) }
    }
}
