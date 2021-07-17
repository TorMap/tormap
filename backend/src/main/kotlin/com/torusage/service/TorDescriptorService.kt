package com.torusage.service

import com.torusage.database.entity.archive.*
import com.torusage.database.repository.archive.ArchiveGeoRelayRepository
import com.torusage.database.repository.archive.ProcessedDescriptorRepository
import com.torusage.database.repository.archive.ProcessedDescriptorsFileRepository
import com.torusage.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.torproject.descriptor.Descriptor
import org.torproject.descriptor.DescriptorCollector
import org.torproject.descriptor.DescriptorSourceFactory
import org.torproject.descriptor.RelayNetworkStatusConsensus
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


/**
 * This service can collect and process Tor descriptors
 */
@Service
class TorDescriptorService(
    val archiveGeoRelayRepository: ArchiveGeoRelayRepository,
    val processedDescriptorsFileRepository: ProcessedDescriptorsFileRepository,
    val processedDescriptorRepository: ProcessedDescriptorRepository,
    val geoLocationService: GeoLocationService,
) {
    val logger = logger()
    val descriptorCollector: DescriptorCollector = DescriptorSourceFactory.createDescriptorCollector()
    val yearMonthDayFormatter = SimpleDateFormat("yyyy-MM-dd")

    @Value("\${collector.api.baseurl}")
    lateinit var collectorApiBaseUrl: String

    @Value("\${collector.target.directory}")
    lateinit var collectorTargetDirectory: String

    /**
     * Collect and process descriptors from a specific the TorProject archive [apiPath]
     */
    fun collectAndProcessDescriptors(apiPath: String, shouldProcessOneDescriptorPerMonth: Boolean) {
        logger.info("Collecting descriptors from api path $apiPath")
        collectDescriptors(apiPath)
        logger.info("Finished collecting descriptors from api path $apiPath")

        logger.info("Processing descriptors from api path $apiPath")
        val parentDirectory = File(collectorTargetDirectory + apiPath)
        val processedFiles = processedDescriptorsFileRepository.findAll()
        parentDirectory.walkBottomUp().forEach {
            processDescriptorsFile(it, parentDirectory, shouldProcessOneDescriptorPerMonth, processedFiles)
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
        shouldOnlyProcessFirstDescriptor: Boolean,
        processedFiles: Iterable<ProcessedDescriptorsFile>,
    ) {
        if (fileToProcess == parentDirectory ||
            processedFiles.any { it.filename == fileToProcess.name && it.lastModified == fileToProcess.lastModified() }
        ) {
            logger.info("Skipping already processed descriptors file ${fileToProcess.name}")
        } else {
            try {
                logger.info("Processing descriptors file ${fileToProcess.name}")
                val descriptorReader = DescriptorSourceFactory.createDescriptorReader()
                if (shouldOnlyProcessFirstDescriptor) {
                    descriptorReader.setMaxDescriptorsInQueue(1)
                    processDescriptor(descriptorReader.readDescriptors(fileToProcess).first())
                } else {
                    descriptorReader.readDescriptors(fileToProcess).forEach {
                        processDescriptor(it)
                    }
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
        when (descriptor) {
            is RelayNetworkStatusConsensus -> processConsensusDescriptor(descriptor)
            else -> throw Exception("Descriptor type ${descriptor.javaClass.name} not supported!")
        }
    }

    /**
     * Process a [descriptor] of the type [RelayNetworkStatusConsensus]
     */
    private fun processConsensusDescriptor(descriptor: RelayNetworkStatusConsensus) {
        val descriptorFileName = descriptor.descriptorFile.name
        val consensusCalendarDate = Calendar.Builder().setInstant(descriptor.validAfterMillis).build()
        val formattedConsensusDate = yearMonthDayFormatter.format(consensusCalendarDate.time)
        val descriptorId = DescriptorId(
            DescriptorType.CONSENSUS,
            consensusCalendarDate
        )
        if (processedDescriptorRepository.existsById(descriptorId)) {
            logger.info("Skipping consensus descriptor for day $formattedConsensusDate part of file $descriptorFileName")
        } else {
            saveArchiveGeoRelays(descriptor, consensusCalendarDate, descriptorId)
            logger.info("Saved consensus descriptor for day $formattedConsensusDate part of file $descriptorFileName")
        }
    }

    /**
     * Use a [descriptor] to generate and save [ArchiveGeoRelay]s
     * based on the relay's IP address and the [consensusCalendarDate]
     */
    private fun saveArchiveGeoRelays(
        descriptor: RelayNetworkStatusConsensus,
        consensusCalendarDate: Calendar,
        descriptorId: DescriptorId,
    ) {
        val nodesToSave = mutableListOf<ArchiveGeoRelay>()
        descriptor.statusEntries.forEach {
            val networkStatusEntry = it.value
            val location = geoLocationService.getLocationForIpAddress(networkStatusEntry.address)
            if (location != null) {
                nodesToSave.add(
                    ArchiveGeoRelay(
                        networkStatusEntry,
                        consensusCalendarDate,
                        location.latitude,
                        location.longitude
                    )
                )
            }
        }
        archiveGeoRelayRepository.saveAll(nodesToSave)
        processedDescriptorRepository.save(ProcessedDescriptor(descriptorId))
    }

}

