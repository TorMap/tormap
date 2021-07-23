package com.torusage.service

import com.torusage.commaSeparatedToList
import com.torusage.database.entity.archive.*
import com.torusage.database.repository.archive.ArchiveGeoRelayRepositoryImpl
import com.torusage.database.repository.archive.ArchiveNodeDetailsRepository
import com.torusage.database.repository.archive.ArchiveNodeFamilyRepository
import com.torusage.database.repository.archive.DescriptorsFileRepository
import com.torusage.jointToCommaSeparated
import com.torusage.logger
import com.torusage.millisSinceEpochToLocalDate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.torproject.descriptor.*
import java.io.File
import java.time.LocalDate
import java.time.YearMonth


/**
 * This service can collect and process Tor descriptors.
 * Descriptors are downloaded from this remote collector endpoint: [https://metrics.torproject.org/collector/]
 */
@Service
class TorDescriptorService(
    val archiveGeoRelayRepositoryImpl: ArchiveGeoRelayRepositoryImpl,
    val archiveNodeDetailsRepository: ArchiveNodeDetailsRepository,
    val descriptorsFileRepository: DescriptorsFileRepository,
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
            collectorApiBaseUrl,
            arrayOf(apiPath),
            minLastModifiedMilliseconds,
            File(collectorTargetDirectory),
            shouldDeleteLocalFilesNotFoundOnRemote,
        )

    /**
     * Process descriptors which were previously saved to disk at [apiPath]
     */
    private fun processDescriptors(apiPath: String, descriptorType: DescriptorType) {
        val processedDescriptorDays = mutableSetOf<LocalDate>()
        var lastProcessedFile: File? = null
        readDescriptors(apiPath).forEach {
            val descriptorDay = processDescriptor(it)
            if (descriptorDay != null) {
                processedDescriptorDays.add(descriptorDay)
            }
            if (lastProcessedFile == null) {
                lastProcessedFile = it.descriptorFile
            } else if (it.descriptorFile != lastProcessedFile) {
                logger.info("Finished processing descriptors file ${lastProcessedFile!!.name}")
                descriptorsFileRepository.save(
                    DescriptorsFile(
                        DescriptorFileId(lastProcessedFile!!.name, descriptorType),
                        lastProcessedFile!!.lastModified()
                    )
                )
                lastProcessedFile = it.descriptorFile
            }
        }
        when (descriptorType) {
            DescriptorType.SERVER -> {
                createArchiveNodeFamilies(processedDescriptorDays.map {
                    YearMonth.from(it).toString()
                }.toSet())
                archiveGeoRelayRepositoryImpl.updateDetailsIds()
            }
            DescriptorType.RELAY_CONSENSUS -> {
                archiveGeoRelayRepositoryImpl.updateDetailsIds()
                archiveGeoRelayRepositoryImpl.updateFamilyIds()
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
    private fun processDescriptor(descriptor: Descriptor): LocalDate? {
        return try {
            when (descriptor) {
                is RelayNetworkStatusConsensus -> processRelayConsensusDescriptor(descriptor)
                is ServerDescriptor -> processServerDescriptor(descriptor)
                else -> throw Exception("Type ${descriptor.javaClass.name} is not supported!")
            }
        } catch (exception: Exception) {
            logger.error("Could not process descriptor part of ${descriptor.descriptorFile.name}: ${exception.message}")
            null
        }
    }

    /**
     * Use a [RelayNetworkStatusConsensus] descriptor to save [ArchiveGeoRelay]s in the DB.
     * The location is retrieved based on the relay's IP addresses.
     */
    private fun processRelayConsensusDescriptor(descriptor: RelayNetworkStatusConsensus): LocalDate {
        val descriptorDay = millisSinceEpochToLocalDate(descriptor.validAfterMillis)
        val nodesToSave = mutableListOf<ArchiveGeoRelay>()
        descriptor.statusEntries.forEach {
            val networkStatusEntry = it.value
            if (!archiveGeoRelayRepositoryImpl.existsByDayAndFingerprint(
                    descriptorDay,
                    networkStatusEntry.fingerprint
                )
            ) {
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
        archiveGeoRelayRepositoryImpl.saveAll(nodesToSave)
        logger.info("Processed relay consensus descriptor for day $descriptorDay")
        return descriptorDay
    }

    /**
     * Use a server descriptor to save [ArchiveNodeDetails] in the DB.
     */
    private fun processServerDescriptor(descriptor: ServerDescriptor): LocalDate {
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
        return descriptorDay
    }

    /**
     * Creates amd saves [ArchiveNodeFamily] entities for the requested [months] by processing [ArchiveNodeDetails].
     */
    private fun createArchiveNodeFamilies(months: Set<String>) {
        val familyNodes = archiveNodeDetailsRepository.getAllByMonthInAndFamilyEntriesNotNull(months)
        familyNodes.forEach { requestingNode ->
            val confirmedFamilyFingerprints = mutableSetOf<String>()
            val month = requestingNode.month
            requestingNode.familyEntries!!.commaSeparatedToList().forEach {
                try {
                    confirmedFamilyFingerprints.add(extractFamilyMemberFingerprint(requestingNode, it, month))
                } catch (exception: Exception) {
                    logger.warn(exception.message)
                }
            }
            if (confirmedFamilyFingerprints.size > 0) {
                confirmedFamilyFingerprints.add(requestingNode.fingerprint)
                saveArchiveNodeFamilies(confirmedFamilyFingerprints, month)
            }
        }
        archiveGeoRelayRepositoryImpl.updateFamilyIds()
    }

    /**
     * Check if an identical [ArchiveNodeFamily] already exists for a given [month], otherwise save it
     */
    private fun saveArchiveNodeFamilies(confirmedFamilyFingerprints: MutableSet<String>, month: String) {
        val sortedFingerprints = confirmedFamilyFingerprints.toSortedSet()
        if (!archiveNodeFamilyRepository.existsByMonthAndFingerprints(
                month,
                sortedFingerprints.jointToCommaSeparated()
            )
        ) {
            archiveNodeFamilyRepository.save(
                ArchiveNodeFamily(
                    sortedFingerprints,
                    month
                )
            )
        }
    }

    /**
     * Extract the fingerprint of a [allegedFamilyMemberId] for a [requestingNode] in a given [month]
     */
    private fun extractFamilyMemberFingerprint(
        requestingNode: ArchiveNodeDetails,
        allegedFamilyMemberId: String,
        month: String,
    ): String {
        val fingerprintRegex = Regex("^\\$[A-F0-9]{40}.*")
        val nicknameRegex = Regex("^[a-zA-Z0-9]{1,19}$")
        when {
            fingerprintRegex.matches(allegedFamilyMemberId) -> {
                val allegedFamilyMember = archiveNodeDetailsRepository.getByMonthAndFingerprint(
                    month,
                    allegedFamilyMemberId.substring(1, 40),
                )
                if (isNodeMemberOfFamily(requestingNode, allegedFamilyMember)) {
                    return allegedFamilyMember!!.fingerprint
                }
            }
            nicknameRegex.matches(allegedFamilyMemberId) -> {
                val allegedFamilyMembers =
                    archiveNodeDetailsRepository.getAllByMonthAndNickname(month, allegedFamilyMemberId)
                val confirmedFamilyMember =
                    allegedFamilyMembers.firstOrNull { isNodeMemberOfFamily(requestingNode, it) }
                if (confirmedFamilyMember != null) {
                    return confirmedFamilyMember.fingerprint
                }
            }
        }
        throw Exception("New family member $allegedFamilyMemberId for requestingNode ${requestingNode.fingerprint} rejected!")
    }

    /**
     * Determines if the [requestingNode] shares a family with the [allegedFamilyMember].
     */
    private fun isNodeMemberOfFamily(
        requestingNode: ArchiveNodeDetails,
        allegedFamilyMember: ArchiveNodeDetails?,
    ) = allegedFamilyMember?.familyEntries?.commaSeparatedToList()?.any {
        it == "$" + requestingNode.fingerprint || it == requestingNode.nickname
    } ?: false
}

