package org.tormap.service

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Caching
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.AsyncResult
import org.springframework.stereotype.Service
import org.tormap.adapter.controller.ArchiveDataController
import org.tormap.config.DescriptorConfig
import org.tormap.database.entity.*
import org.tormap.database.repository.DescriptorsFileRepository
import org.tormap.database.repository.GeoRelayRepositoryImpl
import org.tormap.database.repository.NodeDetailsRepository
import org.tormap.logger
import org.tormap.millisSinceEpochToLocalDate
import org.torproject.descriptor.Descriptor
import org.torproject.descriptor.DescriptorCollector
import org.torproject.descriptor.RelayNetworkStatusConsensus
import org.torproject.descriptor.ServerDescriptor
import org.torproject.descriptor.impl.DescriptorReaderImpl
import org.torproject.descriptor.index.DescriptorIndexCollector
import java.io.File
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.concurrent.Future


/**
 * This service can collect and process Tor descriptors.
 * Descriptors are downloaded from this remote collector endpoint: [https://metrics.torproject.org/collector/]
 */
@Service
class TorDescriptorService(
    private val descriptorConfig: DescriptorConfig,
    private val geoRelayRepositoryImpl: GeoRelayRepositoryImpl,
    private val nodeDetailsRepository: NodeDetailsRepository,
    private val descriptorsFileRepository: DescriptorsFileRepository,
    private val ipLookupService: IpLookupService,
    private val nodeDetailsService: NodeDetailsService,
    private val archiveDataController: ArchiveDataController,
) {
    private val logger = logger()
    private val descriptorCollector: DescriptorCollector = DescriptorIndexCollector()

    /**
     * Collect and process descriptors from a specific the TorProject collector [apiPath]
     */
    fun collectAndProcessDescriptors(apiPath: String, descriptorType: DescriptorType) {
        try {
            logger.info("Collecting descriptors from api path: $apiPath ...")
            collectDescriptors(apiPath, descriptorType.isRecent())
            logger.info("Finished collecting descriptors from api path: $apiPath")

            logger.info("Processing descriptors from api path $apiPath ...")
            processDescriptors(apiPath, descriptorType)
            logger.info("Finished processing descriptors from api path: $apiPath")
        } catch (exception: Exception) {
            logger.error("Could not collect or process descriptors from api path: $apiPath ! ${exception.message}")
        }
    }

    /**
     * This is a wrapper function to collect descriptors from the configured collector API.
     */
    private fun collectDescriptors(
        apiPath: String,
        shouldDeleteLocalFilesNotFoundOnRemote: Boolean
    ) =
        descriptorCollector.collectDescriptors(
            descriptorConfig.apiBaseURL,
            arrayOf(apiPath),
            0L,
            File(descriptorConfig.localDownloadDirectory),
            shouldDeleteLocalFilesNotFoundOnRemote,
        )

    /**
     * Process descriptors which were previously saved to disk at [apiPath]
     */
    private fun processDescriptors(apiPath: String, descriptorType: DescriptorType) {
        var descriptorDaysBeingProcessed = mutableSetOf<Future<ProcessedDescriptorInfo>>()
        var lastProcessedFile: File? = null
        readDescriptors(apiPath, descriptorType).forEach {
            if (lastProcessedFile == null) {
                lastProcessedFile = it.descriptorFile
            } else if (it.descriptorFile != lastProcessedFile) {
                finishDescriptorFile(lastProcessedFile!!, descriptorType, descriptorDaysBeingProcessed)
                lastProcessedFile = it.descriptorFile
                descriptorDaysBeingProcessed = mutableSetOf()
            }
            descriptorDaysBeingProcessed.add(processDescriptor(it))
        }
    }

    /**
     * Waits until all descriptors of the [descriptorFile] are processed and finally saves finished [DescriptorsFile].
     * Updates the [NodeDetails.familyId] of processed months when [descriptorType] is [DescriptorType.ARCHIVE_RELAY_SERVER].
     */
    @Async
    fun finishDescriptorFile(
        descriptorFile: File,
        descriptorType: DescriptorType,
        descriptorDaysBeingProcessed: MutableSet<Future<ProcessedDescriptorInfo>>
    ) {
        val processedMonths = mutableSetOf<String>()
        var lastError: String? = null
        descriptorDaysBeingProcessed.forEach {
            lastError = try {
                val processedDescriptor = it.get()
                if (processedDescriptor.descriptorDay != null) {
                    processedMonths.add(YearMonth.from(processedDescriptor.descriptorDay).toString())
                }
                processedDescriptor.error
            } catch (exception: Exception) {
                val message = "Could not finish processing a descriptor! ${exception.message}"
                logger.error(message)
                message
            } ?: lastError
        }
        if (descriptorType == DescriptorType.ARCHIVE_RELAY_SERVER) { // TODO handle recent relay servers
            nodeDetailsService.updateNodeFamilies(processedMonths)
            nodeDetailsService.updateAutonomousSystems(processedMonths)
        }
        val descriptorsFileId = DescriptorsFileId(descriptorType, descriptorFile.name)
        val descriptorsFile = descriptorsFileRepository.findById(descriptorsFileId).orElseGet {
            DescriptorsFile(
                descriptorsFileId,
                descriptorFile.lastModified(),
            )
        }
        descriptorsFile.processedAt = LocalDateTime.now()
        descriptorsFile.error = lastError
        descriptorsFileRepository.save(descriptorsFile)
        logger.info("Finished processing descriptors file ${descriptorFile.name}")
    }

    /**
     * Read descriptors which were previously saved to disk at [apiPath].
     * A reader can consume quite some memory. Try not to create multiple readers in a short time.
     */
    private fun readDescriptors(apiPath: String, descriptorType: DescriptorType): MutableIterable<Descriptor> {
        val descriptorReader = DescriptorReaderImpl()
        val parentDirectory = File(descriptorConfig.localDownloadDirectory + apiPath)
        if (descriptorType.isRecent()) {
            descriptorsFileRepository.deleteAllById_TypeEqualsAndLastModifiedAfter(
                descriptorType,
                Instant.now().minus(4, ChronoUnit.DAYS).toEpochMilli()
            )
        }
        val excludedFiles = descriptorsFileRepository.findAllById_TypeEqualsAndErrorNull(descriptorType)
        descriptorReader.excludedFiles = excludedFiles.associate {
            Pair(
                parentDirectory.absolutePath + File.separator + it.id.filename,
                it.lastModified,
            )
        }.toSortedMap()

        return descriptorReader.readDescriptors(parentDirectory)
    }

    /**
     * Process a [descriptor] depending on it's type
     */
    @Async
    fun processDescriptor(descriptor: Descriptor): Future<ProcessedDescriptorInfo> {
        return try {
            return when (descriptor) {
                is RelayNetworkStatusConsensus -> AsyncResult(processRelayConsensusDescriptor(descriptor))
                is ServerDescriptor -> AsyncResult(processServerDescriptor(descriptor))
                else -> throw Exception("Type ${descriptor.javaClass.name} is not supported!")
            }
        } catch (exception: Exception) {
            logger.error("Could not process descriptor part of ${descriptor.descriptorFile.name} ! ${exception.message}")
            AsyncResult(ProcessedDescriptorInfo(error = exception.message))
        }
    }

    /**
     * Use a [RelayNetworkStatusConsensus] descriptor to save [GeoRelay]s in the DB.
     * The location is retrieved based on the relay's IP addresses.
     */
    private fun processRelayConsensusDescriptor(descriptor: RelayNetworkStatusConsensus): ProcessedDescriptorInfo {
        val descriptorDay = millisSinceEpochToLocalDate(descriptor.validAfterMillis)
        val nodesToSave = mutableListOf<GeoRelay>()
        descriptor.statusEntries.forEach {
            val networkStatusEntry = it.value
            if (!geoRelayRepositoryImpl.existsByDayAndFingerprint(
                    descriptorDay,
                    networkStatusEntry.fingerprint
                )
            ) {
                val location = ipLookupService.getLocationForIpAddress(networkStatusEntry.address)
                val geoDecimalPlaces = 4
                if (location != null) {
                    nodesToSave.add(
                        GeoRelay(
                            networkStatusEntry,
                            descriptorDay,
                            location.latitude!!.toBigDecimal().setScale(geoDecimalPlaces, RoundingMode.HALF_EVEN),
                            location.longitude!!.toBigDecimal().setScale(geoDecimalPlaces, RoundingMode.HALF_EVEN),
                            location.countryShort,
                        )
                    )
                }
            }
        }
        geoRelayRepositoryImpl.saveAll(nodesToSave)
        updateGeoRelayCaches(descriptorDay.toString())
        return ProcessedDescriptorInfo(descriptorDay)
    }

    @Async
    @Caching(
        evict = [
            CacheEvict("geo-relay-days"),
            CacheEvict("geo-relay-day", key = "#day")
        ]
    )
    fun updateGeoRelayCaches(day: String) {
        archiveDataController.getDaysForGeoRelays()
        archiveDataController.getGeoRelaysByDay(day)
    }

    /**
     * Use a server descriptor to save [NodeDetails] in the DB.
     * Only saves a node if no more recent matching fingerprint is found.
     */
    private fun processServerDescriptor(descriptor: ServerDescriptor): ProcessedDescriptorInfo {
        val descriptorDay = millisSinceEpochToLocalDate(descriptor.publishedMillis)
        val descriptorMonth = YearMonth.from(descriptorDay).toString()
        val existingNode =
            nodeDetailsRepository.findByMonthAndFingerprint(descriptorMonth, descriptor.fingerprint)
        if (existingNode == null || existingNode.day < descriptorDay) {
            nodeDetailsRepository.save(
                NodeDetails(
                    descriptor,
                    descriptorMonth,
                    descriptorDay,
                    existingNode?.id,
                )
            )
        }
        return ProcessedDescriptorInfo(descriptorDay)
    }
}

class ProcessedDescriptorInfo(
    var descriptorDay: LocalDate? = null,
    var error: String? = null,
)

