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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


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

    @Value("\${collector.api.baseurl}")
    lateinit var collectorApiBaseUrl: String

    @Value("\${collector.target.directory}")
    lateinit var collectorTargetDirectory: String

    /**
     * Collect and process descriptors from a specific the TorProject collector [apiPath]
     */
    fun collectAndProcessDescriptors(apiPath: String) {
        try {
            logger.info("Collecting descriptors from api path $apiPath")
            collectDescriptors(apiPath)
            logger.info("Finished collecting descriptors from api path $apiPath")

            logger.info("Processing descriptors from api path $apiPath")
            processDescriptors(apiPath)
            logger.info("Finished processing descriptors from api path $apiPath")
        } catch (exception: Exception) {
            logger.error("Could not collect and process descriptors from api path $apiPath. ${exception.message}")
        }

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
     * Process descriptors which were previously saved to disk at [apiPath]
     */
    private fun processDescriptors(apiPath: String) {
        var lastProcessedFile: File? = null
        readDescriptors(apiPath).forEach {
            processDescriptor(it)
            if (lastProcessedFile == null) {
                lastProcessedFile = it.descriptorFile
            }
            else if (it.descriptorFile != lastProcessedFile) {
                processedDescriptorsFileRepository.save(
                    ProcessedDescriptorsFile(
                        lastProcessedFile!!.name,
                        lastProcessedFile!!.lastModified()
                    )
                )
                lastProcessedFile = it.descriptorFile
            }
        }
    }

    /**
     * Read descriptors which were previously saved to disk at [apiPath]
     * A reader can consume quite some memory. Try not to create multiple readers in a short time.
     */
    private fun readDescriptors(apiPath: String): MutableIterable<Descriptor> {
        val descriptorReader = DescriptorSourceFactory.createDescriptorReader()
        val parentDirectory = File(collectorTargetDirectory + apiPath)
        val excludedFiles = processedDescriptorsFileRepository.findAll()
        descriptorReader.excludedFiles = excludedFiles.associate {
            Pair(
                parentDirectory.absolutePath + File.separator + it.filename,
                it.lastModified,
            )
        }.toSortedMap()

        return descriptorReader.readDescriptors(parentDirectory)
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
        val consensusDate = LocalDate.ofInstant(Instant.ofEpochMilli(descriptor.validAfterMillis), ZoneId.of("UTC"))
        val descriptorId = DescriptorId(
            DescriptorType.CONSENSUS,
            consensusDate
        )
        if (processedDescriptorRepository.existsById(descriptorId)) {
            logger.info("Skipping consensus descriptor for day $consensusDate part of file $descriptorFileName")
        } else {
            saveArchiveGeoRelays(descriptor, consensusDate, descriptorId)
            logger.info("Saved consensus descriptor for day $consensusDate part of file $descriptorFileName")
        }
    }

    /**
     * Use a [descriptor] to generate and save [ArchiveGeoRelay]s
     * based on the relay's IP address and the [consensusDate].
     */
    private fun saveArchiveGeoRelays(
        descriptor: RelayNetworkStatusConsensus,
        consensusDate: LocalDate,
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
                        consensusDate,
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

