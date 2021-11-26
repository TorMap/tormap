package org.tormap.service

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Caching
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.AsyncResult
import org.springframework.stereotype.Service
import org.tormap.CacheName
import org.tormap.adapter.controller.ArchiveDataController
import org.tormap.config.value.DescriptorConfig
import org.tormap.database.entity.*
import org.tormap.database.repository.ProcessedFileRepository
import org.tormap.database.repository.RelayLocationRepositoryImpl
import org.tormap.database.repository.RelayDetailsRepository
import org.tormap.logger
import org.tormap.millisSinceEpochToLocalDate
import org.torproject.descriptor.Descriptor
import org.torproject.descriptor.DescriptorCollector
import org.torproject.descriptor.RelayNetworkStatusConsensus
import org.torproject.descriptor.ServerDescriptor
import org.torproject.descriptor.impl.DescriptorReaderImpl
import org.torproject.descriptor.index.DescriptorIndexCollector
import java.io.File
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
    private val relayLocationRepositoryImpl: RelayLocationRepositoryImpl,
    private val relayDetailsRepository: RelayDetailsRepository,
    private val processedFileRepository: ProcessedFileRepository,
    private val ipLookupService: IpLookupService,
    private val relayDetailsService: RelayDetailsService,
    private val archiveDataController: ArchiveDataController,
) {
    private val logger = logger()
    private val descriptorCollector: DescriptorCollector = DescriptorIndexCollector()

    /**
     * Collect and process descriptors from a specific the TorProject collector [apiPath]
     */
    fun collectAndProcessDescriptors(apiPath: String, descriptorType: DescriptorType) {
        try {
            logger.info("Collecting descriptors from API path: $apiPath ...")
            collectDescriptors(apiPath, descriptorType.isRecent())
            logger.info("Finished collecting descriptors from API path: $apiPath")

            logger.info("Processing descriptors from API path $apiPath ...")
            processDescriptors(apiPath, descriptorType)
            logger.info("Finished processing descriptors from API path: $apiPath")
        } catch (exception: Exception) {
            logger.error("Could not collect or process descriptors from API path: $apiPath ! ${exception.message}")
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
        val processedMonths = mutableSetOf<String>()
        var lastProcessedFile: File? = null
        readDescriptors(apiPath, descriptorType).forEach {
            if (lastProcessedFile == null) {
                lastProcessedFile = it.descriptorFile
            } else if (it.descriptorFile != lastProcessedFile) {
                processedMonths.addAll(
                    finishDescriptorFile(
                        lastProcessedFile!!,
                        descriptorType,
                        descriptorDaysBeingProcessed
                    )
                )
                lastProcessedFile = it.descriptorFile
                descriptorDaysBeingProcessed = mutableSetOf()
            }
            descriptorDaysBeingProcessed.add(processDescriptor(it))
        }
        if (descriptorType === DescriptorType.RECENT_RELAY_SERVER) {
            updateRelayDetails(processedMonths)
        }
    }

    /**
     * Waits until all descriptors of the [descriptorFile] are processed and finally saves finished [ProcessedFile].
     * Updates the [RelayDetails.familyId] of processed months when [descriptorType] is [DescriptorType.ARCHIVE_RELAY_SERVER].
     */
    @Async
    fun finishDescriptorFile(
        descriptorFile: File,
        descriptorType: DescriptorType,
        descriptorDaysBeingProcessed: MutableSet<Future<ProcessedDescriptorInfo>>
    ): MutableSet<String> {
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
        if (descriptorType == DescriptorType.ARCHIVE_RELAY_SERVER) {
            updateRelayDetails(processedMonths)
        }
        saveFinishedDescriptorFile(descriptorFile, descriptorType, lastError)
        return processedMonths
    }

    private fun updateRelayDetails(processedMonths: MutableSet<String>) {
        relayDetailsRepository.flush()
        relayDetailsService.updateFamilies(processedMonths)
        relayDetailsService.updateAutonomousSystems(processedMonths)
    }

    private fun saveFinishedDescriptorFile(descriptorFile: File, descriptorType: DescriptorType, error: String?) {
        val descriptorsDescriptorFileId = DescriptorFileId(descriptorType, descriptorFile.name)
        val descriptorsFile = processedFileRepository.findById(descriptorsDescriptorFileId).orElseGet {
            ProcessedFile(
                descriptorsDescriptorFileId,
                descriptorFile.lastModified(),
            )
        }
        descriptorsFile.processedAt = LocalDateTime.now()
        descriptorsFile.error = error
        processedFileRepository.saveAndFlush(descriptorsFile)
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
            processedFileRepository.deleteAllById_TypeEqualsAndLastModifiedBefore(
                descriptorType,
                Instant.now().minus(4, ChronoUnit.DAYS).toEpochMilli()
            )
        }
        val excludedFiles = processedFileRepository.findAllById_TypeEqualsAndErrorNull(descriptorType)
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
     * Use a [RelayNetworkStatusConsensus] descriptor to save [RelayLocation]s in the DB.
     * The location is retrieved based on the relay's IP addresses.
     */
    private fun processRelayConsensusDescriptor(descriptor: RelayNetworkStatusConsensus): ProcessedDescriptorInfo {
        val descriptorDay = millisSinceEpochToLocalDate(descriptor.validAfterMillis)
        descriptor.statusEntries.forEach {
            val networkStatusEntry = it.value
            if (!relayLocationRepositoryImpl.existsByDayAndFingerprint(
                    descriptorDay,
                    networkStatusEntry.fingerprint
                )
            ) {
                val location = ipLookupService.lookupLocation(networkStatusEntry.address)
                if (location != null) {
                    relayLocationRepositoryImpl.save(RelayLocation(
                        networkStatusEntry,
                        descriptorDay,
                        location.latitude,
                        location.longitude,
                        location.countryCode,
                    ))
                }
            }
        }
        relayLocationRepositoryImpl.flush()
        updateRelayLocationCaches(descriptorDay.toString())
        return ProcessedDescriptorInfo(descriptorDay)
    }

    @Async
    @Caching(
        evict = [
            CacheEvict(CacheName.RELAY_LOCATION_DAYS),
            CacheEvict(CacheName.RELAY_LOCATION_DAY, key = "#day")
        ]
    )
    fun updateRelayLocationCaches(day: String) {
        archiveDataController.getDaysForGeoRelays()
        archiveDataController.getGeoRelaysByDay(day)
    }

    /**
     * Use a server descriptor to save [RelayDetails] in the DB.
     * Only saves a relay if no more recent matching fingerprint is found.
     */
    private fun processServerDescriptor(descriptor: ServerDescriptor): ProcessedDescriptorInfo {
        val descriptorDay = millisSinceEpochToLocalDate(descriptor.publishedMillis)
        val descriptorMonth = YearMonth.from(descriptorDay).toString()
        val existingRelay =
            relayDetailsRepository.findByMonthAndFingerprint(descriptorMonth, descriptor.fingerprint)
        if (existingRelay == null || existingRelay.day < descriptorDay) {
            val autonomousSystem = ipLookupService.lookupAutonomousSystem(descriptor.address)
            relayDetailsRepository.save(
                RelayDetails(
                    descriptor,
                    descriptorMonth,
                    descriptorDay,
                    autonomousSystem?.autonomousSystemOrganization,
                    autonomousSystem?.autonomousSystemNumber,
                    existingRelay?.id,
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

