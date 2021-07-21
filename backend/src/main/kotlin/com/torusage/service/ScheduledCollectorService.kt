package com.torusage.service

import com.torusage.adapter.client.OnionooApiClient
import com.torusage.database.repository.recent.BridgeRepository
import com.torusage.database.repository.recent.BridgeSummaryRepository
import com.torusage.database.repository.recent.RelayRepository
import com.torusage.database.repository.recent.RelaySummaryRepository
import com.torusage.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


/**
 * This scheduler sets reoccurring events to collect data about Tor nodes
 */
@Component
class ScheduledCollectorService(
    val onionooApiClient: OnionooApiClient,
    val relayRepository: RelayRepository,
    val bridgeRepository: BridgeRepository,
    val relaySummaryRepository: RelaySummaryRepository,
    val bridgeSummaryRepository: BridgeSummaryRepository,
    val torDescriptorService: TorDescriptorService,
) {
    val logger = logger()

    @Value("\${collector.path.relay.consensuses}")
    lateinit var collectorPathRelayConsensuses: String

    @Value("\${collector.path.bridge.statuses}")
    lateinit var collectorPathBridgeConsensuses: String

    @Value("\${collector.path.relay.servers}")
    lateinit var collectorApiPathServerDescriptors: String

    /**
     * Fetches and processes relay consensus descriptors.
     * The years 2007 - 2021 equal roughly 3 GB in size.
     */
//    @Scheduled(fixedRate = 86400000L)
    fun processRelayConsensusDescriptors() =
        torDescriptorService.collectAndProcessDescriptors(collectorPathRelayConsensuses)

    /**
     * Fetches and processes bridge network descriptors.
     * The years 2008 - 2021 equal roughly 2 GB in size.
     */
//    @Scheduled(fixedRate = 86400000L)
    fun processBridgeNetworkDescriptors() =
        torDescriptorService.collectAndProcessDescriptors(collectorPathBridgeConsensuses)

    /**
     * Fetches and processes relay server descriptors.
     * The years 2005 - 2021 equal roughly 30 GB in size.
     */
        @Scheduled(fixedRate = 86400000L)
    fun collectRelayServerDescriptors() =
        torDescriptorService.collectAndProcessDescriptors(collectorApiPathServerDescriptors)

    /**
     * Fetches Tor node summary with the configured fixedRate and stores corresponding entities in DB.
     */
//    @Scheduled(fixedRate = 86400000L)
    fun collectOnionooNodeSummary() {
        logger.info("Fetching Onionoo node summary")
        val summaryResponse = onionooApiClient.getTorNodeSummary()
        relaySummaryRepository.saveAll(summaryResponse.relays)
        bridgeSummaryRepository.saveAll(summaryResponse.bridges)
        logger.info("Stored Onionoo node summary in DB")
    }

    /**
     * Fetches Tor node details with the configured fixedRate and stores corresponding entities in DB.
     */
//    @Scheduled(fixedRate = 86400000L)
    fun collectOnionooNodeDetails() {
        logger.info("Fetching Onionoo node details")
        val detailsResponse = onionooApiClient.getTorNodeDetails()
        relayRepository.saveAll(detailsResponse.relays)
        bridgeRepository.saveAll(detailsResponse.bridges)
        logger.info("Stored Onionoo node details in DB")
    }
}

