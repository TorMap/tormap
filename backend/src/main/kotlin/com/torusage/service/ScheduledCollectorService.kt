package com.torusage.service

import com.torusage.adapter.client.OnionooApiClient
import com.torusage.database.entity.DescriptorFile
import com.torusage.database.entity.GeoNode
import com.torusage.database.repository.*
import com.torusage.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.torproject.descriptor.*
import java.io.File
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
    val geoNodeRepository: GeoNodeRepository,
    val descriptorFileRepository: DescriptorFileRepository,
    val geoLocationService: GeoLocationService,
) {
    val logger = logger()
    val descriptorCollector: DescriptorCollector = DescriptorSourceFactory.createDescriptorCollector()
    val descriptorReader: DescriptorReader = DescriptorSourceFactory.createDescriptorReader()

    @Value("\${collector.api.baseurl}")
    lateinit var collectorApiBaseUrl: String

    @Value("\${collector.api.path.consensuses}")
    lateinit var collectorApiPathConsensuses: String

    @Value("\${collector.api.path.servers}")
    lateinit var collectorApiPathServerDescriptors: String

    @Value("\${collector.target.directory}")
    lateinit var collectorTargetDirectory: String

    /**
     * Fetches consensus descriptors and stores them as files
     * The years 2007 - 2021 equal roughly 3 GB in size
     */
    @Scheduled(fixedRate = 86400000L)
    fun collectConsensusesDescriptors() {
        logger.info("Fetching consensus descriptors")
        collectDescriptors(collectorApiPathConsensuses)
        logger.info("Stored consensus descriptors")

        logger.info("Processing descriptors for DB")
        val excludedFiles = mutableMapOf<String, Long>()
        descriptorFileRepository.findAll().forEach { excludedFiles[it.filename] = it.time }
        descriptorReader.excludedFiles = excludedFiles.toSortedMap()

        descriptorReader.readDescriptors(
            File(collectorTargetDirectory + collectorApiPathConsensuses)
        ).forEach { processConsensusesDescriptor(it) }

        descriptorReader.parsedFiles.forEach {
            descriptorFileRepository.save(DescriptorFile(it.key, it.value))
        }
        logger.info("Finished processing descriptors for DB")
    }

    /**
     * Fetches server descriptors and stores them in files
     * The years 2005 - 2021 equal roughly 30 GB in size
     * TODO Currently not scheduled due to large download size
     */
    fun collectServerDescriptors() {
        logger.info("Fetching server descriptors")
        collectDescriptors(collectorApiPathServerDescriptors)
        logger.info("Stored server descriptors")
    }

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

    /**
     * This is a wrapper function to collect descriptors from the configured collector API.
     */
    private fun collectDescriptors(
        apiPath: String,
        minLastModifiedMilliseconds: Long = 0L,
        shouldDeleteLocalFilesNotFoundOnRemote: Boolean = false
    ) =
        descriptorCollector.collectDescriptors(
            collectorApiBaseUrl,
            arrayOf(apiPath),
            minLastModifiedMilliseconds,
            File(collectorTargetDirectory),
            shouldDeleteLocalFilesNotFoundOnRemote,
        )

    /**
     * Process a descriptor which is part of a consensuses file
     */
    private fun processConsensusesDescriptor(descriptor: Descriptor) {
        val descriptorFileName = descriptor.descriptorFile.name
        logger.info("Processing descriptor with size ${descriptor.rawDescriptorLength} which is part of file $descriptorFileName")
        if (descriptor is RelayNetworkStatusConsensus) {
            val nodesToSave = mutableListOf<GeoNode>()
            val consensusDate = Date(descriptor.validAfterMillis)

            descriptor.statusEntries.forEach {
                val networkStatusEntry = it.value
                val location = geoLocationService.getLocationForIpAddress(networkStatusEntry.address)
                if (location != null) {
                    nodesToSave.add(
                        GeoNode(networkStatusEntry, consensusDate, location.longitude, location.latitude)
                    )
                }
            }
            geoNodeRepository.saveAll(nodesToSave)
        }
    }
}

