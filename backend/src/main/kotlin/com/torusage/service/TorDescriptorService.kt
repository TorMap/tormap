package com.torusage.service

import com.torusage.commaSeparatedToList
import com.torusage.database.entity.archive.ArchiveGeoRelay
import com.torusage.database.entity.archive.ArchiveNodeDetails
import com.torusage.database.entity.archive.ArchiveNodeFamily
import com.torusage.database.entity.archive.ProcessedDescriptorsFile
import com.torusage.database.repository.archive.ArchiveGeoRelayRepository
import com.torusage.database.repository.archive.ArchiveNodeDetailsRepository
import com.torusage.database.repository.archive.ArchiveNodeFamilyRepository
import com.torusage.database.repository.archive.ProcessedDescriptorsFileRepository
import com.torusage.jointToCommaSeparated
import com.torusage.logger
import com.torusage.millisSinceEpochToLocalDate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.torproject.descriptor.*
import java.io.File
import java.time.YearMonth


/**
 * This service can collect and process Tor descriptors
 */
@Service
class TorDescriptorService(
    val archiveGeoRelayRepository: ArchiveGeoRelayRepository,
    val archiveNodeDetailsRepository: ArchiveNodeDetailsRepository,
    val processedDescriptorsFileRepository: ProcessedDescriptorsFileRepository,
    val archiveNodeFamilyRepository: ArchiveNodeFamilyRepository,
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
            } else if (it.descriptorFile != lastProcessedFile) {
                logger.info("Finished processing descriptors file ${lastProcessedFile!!.name}")
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
        try {
            when (descriptor) {
                is RelayNetworkStatusConsensus -> processRelayConsensusDescriptor(descriptor)
                is ServerDescriptor -> processServerDescriptor(descriptor)
                else -> throw Exception("Type ${descriptor.javaClass.name} is not supported!")
            }
        } catch (exception: Exception) {
            logger.error("Could not process descriptor part of ${descriptor.descriptorFile.name}: ${exception.message}")
        }
    }

    /**
     * Use a [RelayNetworkStatusConsensus] descriptor to save [ArchiveGeoRelay]s in the DB.
     * The location is retrieved based on the relay's IP addresses.
     */
    private fun processRelayConsensusDescriptor(descriptor: RelayNetworkStatusConsensus) {
        val descriptorDay = millisSinceEpochToLocalDate(descriptor.validAfterMillis)
        val nodesToSave = mutableListOf<ArchiveGeoRelay>()
        descriptor.statusEntries.forEach {
            val networkStatusEntry = it.value
            if (!archiveGeoRelayRepository.existsByDayAndFingerprint(descriptorDay, networkStatusEntry.fingerprint)) {
                val location = geoLocationService.getLocationForIpAddress(networkStatusEntry.address)
                if (location != null) {
                    nodesToSave.add(
                        ArchiveGeoRelay(
                            networkStatusEntry,
                            descriptorDay,
                            location.latitude,
                            location.longitude,
                            location.countryIsoCode,
                        )
                    )
                }
            }
        }
        archiveGeoRelayRepository.saveAll(nodesToSave)
        logger.info("Processed relay consensus descriptor for day $descriptorDay")
    }

    /**
     * Use a server descriptor to save [ArchiveNodeDetails] in the DB.
     */
    private fun processServerDescriptor(descriptor: ServerDescriptor) {
        val descriptorDay = millisSinceEpochToLocalDate(descriptor.publishedMillis)
        val descriptorMonth = YearMonth.from(descriptorDay).toString()
        val existingNode =
            archiveNodeDetailsRepository.getByMonthAndFingerprint(descriptorMonth, descriptor.fingerprint)
        if (existingNode == null || existingNode.day < descriptorDay) {
            archiveNodeDetailsRepository.save(
                ArchiveNodeDetails(
                    descriptor,
                    descriptorMonth,
                    descriptorDay,
                    existingNode?.id
                )
            )
        }
    }

    fun analyzeNodeFamilies(month: String) {
        val familyNodes = archiveNodeDetailsRepository.getAllByMonthAndFamilyEntriesIsNotNull(month)
        familyNodes.forEach { requestingNode ->
            val confirmedFamilyFingerprints = mutableSetOf<String>()
            requestingNode.familyEntries!!.commaSeparatedToList().forEach { allegedFamilyMemberId ->
                val fingerprintRegex = Regex("^\\$[A-F0-9]{40}.*")
                val nicknameRegex = Regex("^[a-zA-Z0-9]{1,19}$")
                when {
                    fingerprintRegex.matches(allegedFamilyMemberId) -> {
                        val allegedFamilyMember = archiveNodeDetailsRepository.getByMonthAndFingerprint(
                            month,
                            allegedFamilyMemberId.substring(1, 40),
                        )
                        if (isNodeMemberOfFamily(requestingNode, allegedFamilyMember)) {
                            confirmedFamilyFingerprints.add(allegedFamilyMember!!.fingerprint)
                        }

                    }
                    nicknameRegex.matches(allegedFamilyMemberId) -> {
                        val allegedFamilyMembers =
                            archiveNodeDetailsRepository.getAllByMonthAndNickname(month, allegedFamilyMemberId)
                        val confirmedFamilyMember =
                            allegedFamilyMembers.firstOrNull { isNodeMemberOfFamily(requestingNode, it) }
                        if (confirmedFamilyMember != null) {
                            confirmedFamilyFingerprints.add(confirmedFamilyMember.fingerprint)
                        }

                    }
                    else -> logger.error("Could not add member $allegedFamilyMemberId to requestingNode ${requestingNode.id} family!")
                }
            }
            if (confirmedFamilyFingerprints.size > 0) {
                confirmedFamilyFingerprints.add(requestingNode.fingerprint)
                val confirmedFamilyFingerprintsSorted = confirmedFamilyFingerprints.toSortedSet()
                if (!archiveNodeFamilyRepository.existsByMonthAndFingerprints(
                        month,
                        confirmedFamilyFingerprintsSorted.jointToCommaSeparated()!!
                    )
                ) {
                    archiveNodeFamilyRepository.save(
                        ArchiveNodeFamily(
                            confirmedFamilyFingerprintsSorted,
                            month
                        )
                    )
                }
            }
        }
    }

    private fun isNodeMemberOfFamily(
        requestingNode: ArchiveNodeDetails,
        allegedFamilyMember: ArchiveNodeDetails?,
    ) = allegedFamilyMember?.familyEntries?.commaSeparatedToList()?.any {
        it == "$" + requestingNode.fingerprint || it == requestingNode.nickname
    } ?: false
}

