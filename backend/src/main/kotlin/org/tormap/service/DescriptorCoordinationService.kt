package org.tormap.service

import org.springframework.stereotype.Service
import org.tormap.config.value.DescriptorConfig
import org.tormap.database.entity.DescriptorType
import org.tormap.database.entity.isRecent
import org.tormap.database.entity.isRelayConsensusType
import org.tormap.database.entity.isRelayServerType
import org.tormap.database.repository.RelayDetailsRepository
import org.tormap.database.repository.RelayLocationRepository
import org.tormap.util.logger
import org.torproject.descriptor.DescriptorCollector
import org.torproject.descriptor.index.DescriptorIndexCollector
import java.io.File


/**
 * This service coordinates the collection and processing of Tor descriptors provided by the TorProject.
 * Descriptors are downloaded from a remote collector endpoint: [https://metrics.torproject.org/collector/]
 */
@Service
class DescriptorCoordinationService(
    private val descriptorConfig: DescriptorConfig,
    private val relayDetailsUpdateService: RelayDetailsUpdateService,
    private val descriptorFileService: DescriptorFileService,
    private val descriptorProcessingService: DescriptorProcessingService,
    private val relayDetailsRepository: RelayDetailsRepository,
    private val relayLocationRepository: RelayLocationRepository,
    private val cacheService: CacheService
) {
    private val logger = logger()
    private val descriptorCollector: DescriptorCollector = DescriptorIndexCollector()

    fun collectAndProcessDescriptors(apiPath: String, descriptorType: DescriptorType) {
        try {
            logger.info("... Collecting descriptors from API path: $apiPath")
            collectDescriptorsFromRemoteServer(apiPath, descriptorType.isRecent())
            logger.info("Finished collecting descriptors from API path: $apiPath")

            logger.info("... Processing descriptors from API path $apiPath")
            processLocalDescriptorFiles(apiPath, descriptorType)
            logger.info("Finished processing descriptors from API path: $apiPath")
        } catch (exception: Exception) {
            logger.error("Could not collect or process descriptors from API path: $apiPath! ${exception.message}")
        }
    }

    private fun collectDescriptorsFromRemoteServer(
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

    private fun processLocalDescriptorFiles(apiPath: String, descriptorType: DescriptorType) {
        var lastProcessedFile: File? = null
        var errorCount = 0
        val processedMonths = mutableSetOf<String>()
        descriptorFileService.getDescriptorDiskReader(apiPath, descriptorType).forEach { descriptor ->
            lastProcessedFile?.let {
                if (it != descriptor.descriptorFile) {
                    flushRelayRepository(descriptorType)
                    saveProcessedFileReference(it, descriptorType, errorCount)
                    updateRelayDetailsAndCaches(descriptorType, processedMonths)
                    processedMonths.clear()
                    errorCount = 0
                }
            }
            val descriptorInfo = descriptorProcessingService.processDescriptor(descriptor)
            descriptorInfo.yearMonth?.let { processedMonths.add(it) }
            descriptorInfo.error?.let { errorCount++ }
            lastProcessedFile = descriptor.descriptorFile
        }
    }

    private fun flushRelayRepository(descriptorType: DescriptorType) {
        try {
            when {
                descriptorType.isRelayServerType() -> relayDetailsRepository.flush()
                descriptorType.isRelayConsensusType() -> relayLocationRepository.flush()
                else -> throw Exception("Descriptor type ${descriptorType.name} is not yet supported!")
            }
        } catch (exception: Exception) {
            logger.error("Could not flush relay repository for ${descriptorType.name}! ${exception.message}")
        }
    }

    private fun saveProcessedFileReference(file: File, descriptorType: DescriptorType, errorCount: Int) {
        if (errorCount == 0) {
            descriptorFileService.saveProcessedFileReference(file, descriptorType)
            logger.info("Finished ${file.name} with 0 errors")
        } else {
            logger.error("Failed ${file.name} with $errorCount errors. Not saving as finished file reference!")
        }
    }

    private fun updateRelayDetailsAndCaches(descriptorType: DescriptorType, processedMonths: Set<String>) {
        if (processedMonths.isNotEmpty()) {
            if (descriptorType.isRelayServerType()) {
                relayDetailsUpdateService.computeFamilies(processedMonths)
                relayDetailsUpdateService.lookupMissingAutonomousSystems(processedMonths)
            }
            if (descriptorType.isRelayConsensusType()) {
                cacheService.cacheRelayLocationDistinctDays()
            }
            if (descriptorType.isRecent()) {
                cacheService.cacheRelayLocationsPerDay(processedMonths)
            } else {
                cacheService.evictRelayLocationsPerDay(processedMonths)
            }
        }
    }
}
