package com.torusage.service

import com.torusage.adapter.client.OnionooApiClient
import com.torusage.database.entity.archive.*
import com.torusage.database.repository.archive.ArchiveGeoRelayRepository
import com.torusage.database.repository.archive.ProcessedDescriptorRepository
import com.torusage.database.repository.archive.ProcessedDescriptorsFileRepository
import com.torusage.database.repository.recent.BridgeRepository
import com.torusage.database.repository.recent.BridgeSummaryRepository
import com.torusage.database.repository.recent.RelayRepository
import com.torusage.database.repository.recent.RelaySummaryRepository
import com.torusage.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.torproject.descriptor.Descriptor
import org.torproject.descriptor.DescriptorCollector
import org.torproject.descriptor.DescriptorSourceFactory
import org.torproject.descriptor.RelayNetworkStatusConsensus
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


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

    @Value("\${collector.api.path.consensuses}")
    lateinit var collectorApiPathConsensuses: String

    @Value("\${collector.api.path.servers}")
    lateinit var collectorApiPathServerDescriptors: String

    /**
     * Fetches consensus descriptors and stores them as files
     * The years 2007 - 2021 equal roughly 3 GB in size
     */
    @Scheduled(fixedRate = 86400000L)
    fun collectConsensusesDescriptors() = torDescriptorService.collectAndProcessDescriptors(
            collectorApiPathConsensuses,
            true,
    )

    /**
     * Fetches server descriptors and stores them in files
     * The years 2005 - 2021 equal roughly 30 GB in size
     */
    //    @Scheduled(fixedRate = 86400000L)
    fun collectServerDescriptors() = torDescriptorService.collectAndProcessDescriptors(
            collectorApiPathServerDescriptors,
            false,
    )

    /**
     * Fetches Tor node summary with the configured fixedRate and stores corresponding entities in DB
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
     * Fetches Tor node details with the configured fixedRate and stores corresponding entities in DB
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

