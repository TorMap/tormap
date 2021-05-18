package com.torusage

import com.torusage.adapter.OnionooApiClient
import com.torusage.common.logger
import com.torusage.database.repository.BridgeRepository
import com.torusage.database.repository.BridgeSummaryRepository
import com.torusage.database.repository.RelayRepository
import com.torusage.database.repository.RelaySummaryRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


@Component
class ScheduledTorNodeManager(
    val onionooApiClient: OnionooApiClient,
    val relayRepository: RelayRepository,
    val bridgeRepository: BridgeRepository,
    val relaySummaryRepository: RelaySummaryRepository,
    val bridgeSummaryRepository: BridgeSummaryRepository,
) {
    /**
     * Fetches Tor node summary every hour and stores corresponding entities in DB.
     */
    @Scheduled(fixedRate = 3600000)
    fun fetchAndStoreTorNodeSummary() {
        logger().info("Fetching Tor node summary")
        val summaryResponse = onionooApiClient.getTorNodeSummary()
        relaySummaryRepository.saveAll(summaryResponse.relays)
        bridgeSummaryRepository.saveAll(summaryResponse.bridges)
        logger().info("Stored Tor node summary in DB")
    }

    /**
     * Fetches Tor node details every hour and stores corresponding entities in DB.
     */
    @Scheduled(fixedRate = 3600000)
    fun fetchAndStoreTorNodeDetails() {
        logger().info("Fetching Tor node details")
        val detailsResponse = onionooApiClient.getTorNodeDetails()
        relayRepository.saveAll(detailsResponse.relays)
        bridgeRepository.saveAll(detailsResponse.bridges)
        logger().info("Stored Tor node details in DB")
    }
}
