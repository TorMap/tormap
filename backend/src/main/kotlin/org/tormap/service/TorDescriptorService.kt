package org.tormap.service

import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.tormap.config.DescriptorConfig
import org.tormap.database.entity.DescriptorType
import org.tormap.database.entity.ProcessedFile
import org.tormap.database.entity.RelayDetails
import org.tormap.database.entity.RelayLocation
import org.tormap.database.repository.ProcessedFileRepository
import org.tormap.database.repository.RelayDetailsRepository
import org.tormap.database.repository.RelayLocationRepository
import org.tormap.util.toLocalDate
import org.torproject.descriptor.Descriptor
import org.torproject.descriptor.DescriptorCollector
import org.torproject.descriptor.RelayNetworkStatusConsensus
import org.torproject.descriptor.ServerDescriptor
import org.torproject.descriptor.UnparseableDescriptor
import org.torproject.descriptor.impl.DescriptorReaderImpl
import org.torproject.descriptor.index.DescriptorIndexCollector
import java.io.File
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.io.path.absolute

/**
 * This service can collect and process Tor descriptors.
 * Descriptors are by default downloaded from a remote collector endpoint: [https://metrics.torproject.org/collector/]
 */
@Service
class TorDescriptorService(
    private val descriptorConfig: DescriptorConfig,
    private val relayLocationRepository: RelayLocationRepository,
    private val relayDetailsRepository: RelayDetailsRepository,
    private val processedFileRepository: ProcessedFileRepository,
    private val ipLookupService: IpLookupService,
    private val relayDetailsUpdateService: RelayDetailsUpdateService
) {

    private val logger = KotlinLogging.logger { }

    private val descriptorCollector: DescriptorCollector = DescriptorIndexCollector()

    /**
     * Collect and process descriptors from a specific the TorProject collector [apiPath]
     */
    fun collectAndProcessDescriptors(apiPath: String, descriptorType: DescriptorType) {
        try {
            logger.info { "... Collecting descriptors from API path: $apiPath" }
            collectDescriptors(apiPath, descriptorType.isRecent())
            logger.info { "Finished collecting descriptors from API path: $apiPath" }

            logger.info { "... Processing descriptors from API path $apiPath" }
            processDescriptors(apiPath, descriptorType)
            logger.info { "Finished processing descriptors from API path: $apiPath" }
        } catch (exception: Exception) {
            logger.error(exception) { "Could not collect or process descriptors from API path: $apiPath!" }
        }
    }

    /**
     * This is a wrapper function to collect descriptors from the configured collector API.
     */
    private fun collectDescriptors(apiPath: String, shouldDeleteLocalFilesNotFoundOnRemote: Boolean) =
        descriptorCollector.collectDescriptors(
            descriptorConfig.apiBaseURL.toString(),
            arrayOf(apiPath),
            0L,
            descriptorConfig.localDownloadDirectory.toFile(),
            shouldDeleteLocalFilesNotFoundOnRemote
        )

    /**
     * Process descriptors which were previously saved to disk at [apiPath]
     */
    private fun processDescriptors(apiPath: String, descriptorType: DescriptorType) {
        val processedDescriptors = mutableListOf<ProcessedDescriptorInfo>()
        val processedMonths = mutableListOf<String>()
        var lastProcessedFile: File? = null

        for (descriptor in readDescriptors(apiPath, descriptorType)) {
            when {
                lastProcessedFile == null -> lastProcessedFile = descriptor.descriptorFile
                descriptor.descriptorFile != lastProcessedFile -> {
                    val finishedFile = finishDescriptorFile(lastProcessedFile, descriptorType, processedDescriptors)
                    processedMonths.addAll(finishedFile)
                    lastProcessedFile = descriptor.descriptorFile
                    processedDescriptors.clear()
                }
            }

            processedDescriptors.add(processDescriptor(descriptor))
        }

        if (descriptorType == DescriptorType.RECENT_RELAY_SERVER) {
            relayDetailsUpdateService.updateFamilies(processedMonths)
        }
    }

    /**
     * Waits until all descriptors of the [descriptorFile] are processed and finally saves finished [ProcessedFile].
     * Updates the [RelayDetails.familyId] of processed months when [descriptorType] is [DescriptorType.ARCHIVE_RELAY_SERVER].
     */
    fun finishDescriptorFile(
        descriptorFile: File,
        descriptorType: DescriptorType,
        descriptorDaysBeingProcessed: List<ProcessedDescriptorInfo>
    ): List<String> {
        val processedMonths = mutableListOf<String>()
        var lastError: String? = null

        for (processedDescriptor in descriptorDaysBeingProcessed) {
            lastError = try {
                if (processedDescriptor.yearMonth != null) {
                    processedMonths.add(processedDescriptor.yearMonth!!)
                }
                processedDescriptor.error
            } catch (exception: Exception) {
                val message = "Could not finish processing a descriptor! ${exception.message}"
                logger.warn(message)
                message
            } ?: lastError
        }

        if (descriptorType == DescriptorType.ARCHIVE_RELAY_SERVER) {
            relayDetailsUpdateService.updateFamilies(processedMonths)
        }
        saveFinishedDescriptorFile(descriptorFile, descriptorType, lastError)
        return processedMonths
    }

    /**
     * Saves a reference of the finished [descriptorFile] to the DB.
     * Next time this [descriptorFile] will be excluded from processing if no [error] was found or a newer version exists.
     */
    private fun saveFinishedDescriptorFile(descriptorFile: File, descriptorType: DescriptorType, error: String?) {
        val descriptorsFile = processedFileRepository.findByIdOrNull(descriptorFile.name)
            ?: ProcessedFile(descriptorFile.name, descriptorType, descriptorFile.lastModified()).apply { setNew() }

        descriptorsFile.processedAt = LocalDateTime.now()
        descriptorsFile.error = error
        processedFileRepository.save(descriptorsFile)
        logger.info { "Finished processing descriptors file ${descriptorFile.name}" }
    }

    /**
     * Read descriptors which were previously saved to disk at [apiPath].
     * A reader can consume quite some memory. Try not to create multiple readers in a short time.
     */
    private fun readDescriptors(apiPath: String, descriptorType: DescriptorType): MutableIterable<Descriptor> {
        val descriptorReader = DescriptorReaderImpl()
        val parentDirectory = Paths.get(descriptorConfig.localDownloadDirectory.toString(), apiPath)
        if (descriptorType.isRecent()) {
            processedFileRepository.deleteAllByTypeAndLastModifiedBefore(
                descriptorType,
                Instant.now().minus(4, ChronoUnit.DAYS).toEpochMilli()
            )
        }
        val excludedFiles = processedFileRepository.findAllByTypeAndErrorNull(descriptorType)
        descriptorReader.excludedFiles = excludedFiles.associate {
            parentDirectory.resolve(it.filename).absolute().toString() to it.lastModified
        }.toSortedMap()

        return descriptorReader.readDescriptors(parentDirectory.toFile())
    }

    /**
     * Process a [descriptor] depending on it's type
     */
    fun processDescriptor(descriptor: Descriptor): ProcessedDescriptorInfo {
        return try {
            when (descriptor) {
                is RelayNetworkStatusConsensus -> processRelayConsensusDescriptor(descriptor)
                is ServerDescriptor -> processServerDescriptor(descriptor)
                is UnparseableDescriptor -> {
                    logger.debug(descriptor.descriptorParseException) {
                        "Unparsable descriptor in file ${descriptor.descriptorFile.name}"
                    }
                    ProcessedDescriptorInfo()
                }

                else -> throw UnsupportedOperationException(
                    "Descriptor type ${descriptor.javaClass.name} is not yet supported!"
                )
            }
        } catch (exception: Exception) {
            logger.error { "Could not process descriptor part of ${descriptor.descriptorFile.name}! ${exception.message}" }
            ProcessedDescriptorInfo(error = exception.message)
        }
    }

    /**
     * Use a [RelayNetworkStatusConsensus] descriptor to save [RelayLocation]s in the DB.
     * The location is retrieved based on the relay's IP addresses.
     */
    private fun processRelayConsensusDescriptor(descriptor: RelayNetworkStatusConsensus): ProcessedDescriptorInfo {
        val descriptorDay = Instant.ofEpochMilli(descriptor.validAfterMillis).toLocalDate()
        for (statusEntry in descriptor.statusEntries) {
            val networkStatusEntry = statusEntry.value
            if (!relayLocationRepository.existsByDayAndFingerprint(descriptorDay, networkStatusEntry.fingerprint)) {
                ipLookupService.lookupLocation(networkStatusEntry.address)?.let {
                    relayLocationRepository.save(
                        RelayLocation(networkStatusEntry, descriptorDay, it.latitude, it.longitude, it.countryCode)
                    )
                }
            }
        }

        return ProcessedDescriptorInfo(YearMonth.from(descriptorDay).toString())
    }

    /**
     * Use a server descriptor to save [RelayDetails] in the DB.
     * Only saves a relay if no more recent matching fingerprint is found.
     */
    private fun processServerDescriptor(descriptor: ServerDescriptor): ProcessedDescriptorInfo {
        val descriptorDay = Instant.ofEpochMilli(descriptor.publishedMillis).toLocalDate()
        val descriptorMonth = YearMonth.from(descriptorDay).toString()
        val existingRelay = relayDetailsRepository.findByMonthAndFingerprint(descriptorMonth, descriptor.fingerprint)
        if (existingRelay == null || existingRelay.day < descriptorDay) {
            val autonomousSystem = ipLookupService.lookupAutonomousSystem(descriptor.address)
            val relayDetails = RelayDetails(
                descriptor,
                descriptorMonth,
                descriptorDay,
                autonomousSystem?.autonomousSystemOrganization,
                autonomousSystem?.autonomousSystemNumber?.toInt()
            )
            relayDetailsRepository.save(existingRelay.update(relayDetails))
        }
        return ProcessedDescriptorInfo(descriptorMonth)
    }
}

data class ProcessedDescriptorInfo(
    var yearMonth: String? = null,
    var error: String? = null
)
