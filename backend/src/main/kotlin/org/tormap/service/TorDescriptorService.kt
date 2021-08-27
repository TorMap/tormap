package org.tormap.service

import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.AsyncResult
import org.springframework.stereotype.Service
import org.tormap.config.ApiConfig
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
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.Future


/**
 * This service can collect and process Tor descriptors.
 * Descriptors are downloaded from this remote collector endpoint: [https://metrics.torproject.org/collector/]
 */
@Service
class TorDescriptorService(
    private val apiConfig: ApiConfig,
    val geoRelayRepositoryImpl: GeoRelayRepositoryImpl,
    val nodeDetailsRepository: NodeDetailsRepository,
    val descriptorsFileRepository: DescriptorsFileRepository,
    val ipLookupService: IpLookupService,
    val nodeDetailsService: NodeDetailsService,
) {
    val logger = logger()
    val descriptorCollector: DescriptorCollector = DescriptorIndexCollector()

    /**
     * Collect and process descriptors from a specific the TorProject collector [apiPath]
     */
    fun collectAndProcessDescriptors(apiPath: String, descriptorType: DescriptorType) {
        try {
            logger.info("Collecting descriptors from api path $apiPath")
            collectDescriptors(apiPath)
            logger.info("Finished collecting descriptors from api path $apiPath")

            logger.info("Processing descriptors from api path $apiPath")
            processDescriptors(apiPath, descriptorType)
            logger.info("Finished processing descriptors from api path $apiPath")
        } catch (exception: Exception) {
            logger.error("Could not collect or process descriptors from api path $apiPath. ${exception.message}")
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
            apiConfig.descriptorBaseURL,
            arrayOf(apiPath),
            minLastModifiedMilliseconds,
            File(apiConfig.descriptorDownloadDirectory),
            shouldDeleteLocalFilesNotFoundOnRemote,
        )

    /**
     * Process descriptors which were previously saved to disk at [apiPath]
     */
    private fun processDescriptors(apiPath: String, descriptorType: DescriptorType) {
        var descriptorDaysBeingProcessed = mutableSetOf<Future<LocalDate?>>()
        var lastProcessedFile: File? = null
        readDescriptors(apiPath).forEach {
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
     * Updates the [NodeDetails.familyId] of processed months when [descriptorType] is [DescriptorType.SERVER].
     */
    @Async
    fun finishDescriptorFile(
        descriptorFile: File,
        descriptorType: DescriptorType,
        descriptorDaysBeingProcessed: MutableSet<Future<LocalDate?>>
    ) {
        val processedMonths = mutableSetOf<String>()
        descriptorDaysBeingProcessed.forEach {
            try {
                val processedDay = it.get()
                if (processedDay != null) {
                    processedMonths.add(YearMonth.from(processedDay).toString())
                }
            } catch (exception: Exception) {
                logger.error("Could not complete processing a descriptor. ${exception.message}")
            }
        }
        if (descriptorType == DescriptorType.SERVER) {
            nodeDetailsService.updateAutonomousSystems(processedMonths)
            nodeDetailsService.updateNodeFamilies(processedMonths)
        }
        descriptorsFileRepository.save(
            DescriptorsFile(
                DescriptorsFileId(descriptorFile.name, descriptorType),
                descriptorFile.lastModified()
            )
        )
        logger.info("Finished processing descriptors file ${descriptorFile.name}")
    }

    /**
     * Read descriptors which were previously saved to disk at [apiPath].
     * A reader can consume quite some memory. Try not to create multiple readers in a short time.
     */
    private fun readDescriptors(apiPath: String): MutableIterable<Descriptor> {
        val descriptorReader = DescriptorReaderImpl()
        val parentDirectory = File(apiConfig.descriptorDownloadDirectory + apiPath)
        val excludedFiles = descriptorsFileRepository.findAll()
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
    fun processDescriptor(descriptor: Descriptor): Future<LocalDate?> {
        return try {
            return when (descriptor) {
                is RelayNetworkStatusConsensus -> AsyncResult(processRelayConsensusDescriptor(descriptor))
                is ServerDescriptor -> AsyncResult(processServerDescriptor(descriptor))
                else -> throw Exception("Type ${descriptor.javaClass.name} is not supported!")
            }
        } catch (exception: Exception) {
            logger.error("Could not process descriptor part of ${descriptor.descriptorFile.name}: ${exception.message}")
            AsyncResult(null)
        }
    }

    /**
     * Use a [RelayNetworkStatusConsensus] descriptor to save [GeoRelay]s in the DB.
     * The location is retrieved based on the relay's IP addresses.
     */
    private fun processRelayConsensusDescriptor(descriptor: RelayNetworkStatusConsensus): LocalDate {
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
        logger.debug("Processed relay consensus descriptor for day $descriptorDay")
        return descriptorDay
    }

    /**
     * Use a server descriptor to save [NodeDetails] in the DB.
     * Only saves a node if no more recent matching fingerprint is found.
     */
    private fun processServerDescriptor(descriptor: ServerDescriptor): LocalDate {
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
        return descriptorDay
    }
}

