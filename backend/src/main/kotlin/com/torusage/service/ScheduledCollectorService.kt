package com.torusage.service

import com.torusage.adapter.client.OnionooApiClient
import com.torusage.database.entity.GeoNode
import com.torusage.database.entity.ProcessedDescriptorsFile
import com.torusage.database.repository.*
import com.torusage.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.torproject.descriptor.Descriptor
import org.torproject.descriptor.DescriptorCollector
import org.torproject.descriptor.DescriptorSourceFactory
import org.torproject.descriptor.RelayNetworkStatusConsensus
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
    val processedDescriptorsFileRepository: ProcessedDescriptorsFileRepository,
    val geoLocationService: GeoLocationService,
) {
    val logger = logger()
    val descriptorCollector: DescriptorCollector = DescriptorSourceFactory.createDescriptorCollector()

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
    fun collectConsensusesDescriptors() = collectAndProcessDescriptors(collectorApiPathConsensuses)

    /**
     * Fetches server descriptors and stores them in files
     * The years 2005 - 2021 equal roughly 30 GB in size
     */
    //    @Scheduled(fixedRate = 86400000L)
    fun collectServerDescriptors() = collectAndProcessDescriptors(collectorApiPathServerDescriptors)

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
     * Collect and process descriptors from a specific the TorProject archive [apiPath]
     */
    private fun collectAndProcessDescriptors(apiPath: String) {
        logger.info("Collecting descriptors from api path $apiPath")
        collectDescriptors(apiPath)
        logger.info("Finished collecting descriptors from api path $apiPath")

        logger.info("Processing descriptors from api path $apiPath")
        val parentDirectory = File(collectorTargetDirectory + collectorApiPathConsensuses)
        val processedFiles = processedDescriptorsFileRepository.findAll()
        parentDirectory.walkBottomUp().forEach {
            processDescriptorsFile(it, parentDirectory, processedFiles)
        }
        logger.info("Finished processing descriptors from api path $apiPath")
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
     * The [fileToProcess] must contain descriptors which are then processed by the [processDescriptor] method
     */
    private fun processDescriptorsFile(
        fileToProcess: File,
        parentDirectory: File,
        processedFiles: Iterable<ProcessedDescriptorsFile>,
    ) {
        if (fileToProcess != parentDirectory &&
            !processedFiles.any { it.filename == fileToProcess.name && it.lastModified != fileToProcess.lastModified() }
        ) {
            try {
                logger.info("Processing descriptors file ${fileToProcess.name}")
                DescriptorSourceFactory.createDescriptorReader().readDescriptors(fileToProcess).forEach {
                    processDescriptor(it)
                }
                processedDescriptorsFileRepository.save(
                    ProcessedDescriptorsFile(
                        fileToProcess.name,
                        fileToProcess.lastModified()
                    )
                )
                logger.info("Finished processing descriptors file ${fileToProcess.name}")
            } catch (exception: Exception) {
                logger.error("Failed to process descriptors file ${fileToProcess.name}. " + exception.message)
            }
        }
    }

    /**
     * Process a [descriptor] depending on it's type
     */
    private fun processDescriptor(descriptor: Descriptor) {
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
                        GeoNode(networkStatusEntry, consensusDate, location.latitude, location.longitude)
                    )
                }
            }
            geoNodeRepository.saveAll(nodesToSave)
        }
    }
}

