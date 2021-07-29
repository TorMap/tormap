package com.torusage.service

import com.torusage.commaSeparatedToList
import com.torusage.config.ApiConfig
import com.torusage.database.entity.*
import com.torusage.database.repository.DescriptorsFileRepository
import com.torusage.database.repository.GeoRelayRepositoryImpl
import com.torusage.database.repository.NodeDetailsRepository
import com.torusage.logger
import com.torusage.millisSinceEpochToLocalDate
import org.springframework.jdbc.support.incrementer.H2SequenceMaxValueIncrementer
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
    private val apiConfig: ApiConfig,
    val geoRelayRepositoryImpl: GeoRelayRepositoryImpl,
    val nodeDetailsRepository: NodeDetailsRepository,
    val descriptorsFileRepository: DescriptorsFileRepository,
    val geoLocationService: GeoLocationService,
    val dbSequenceIncrementer: H2SequenceMaxValueIncrementer,
) {
    val logger = logger()
    val descriptorCollector: DescriptorCollector = DescriptorSourceFactory.createDescriptorCollector()

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
     * Updates [GeoRelay.nodeDetailsId] and [GeoRelay.nodeFamilyId] for all [GeoRelay]s
     */
    fun updateAllGeoRelayForeignIds() {
        try {
            logger.info("Creating node families for all available months")
            createNodeFamilies()
        } catch (exception: Exception) {
            logger.error("Could not update all geo relay foreign ids. ${exception.message}")
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
                        DescriptorsFileId(lastProcessedFile!!.name, descriptorType),
                        lastProcessedFile!!.lastModified()
                    )
                )
                lastProcessedFile = it.descriptorFile
            }
        }
        when (descriptorType) {
            DescriptorType.SERVER -> {
                createNodeFamilies(processedDescriptorDays.map {
                    YearMonth.from(it).toString()
                }.toSet())
            }
            DescriptorType.RELAY_CONSENSUS -> {
                geoRelayRepositoryImpl.updateForeignIds()
            }
        }
    }

    /**
     * Read descriptors which were previously saved to disk at [apiPath]
     * A reader can consume quite some memory. Try not to create multiple readers in a short time.
     */
    private fun readDescriptors(apiPath: String): MutableIterable<Descriptor> {
        val descriptorReader = DescriptorSourceFactory.createDescriptorReader()
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
                val location = geoLocationService.getLocationForIpAddress(networkStatusEntry.address)
                if (location != null) {
                    nodesToSave.add(
                        GeoRelay(
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
        geoRelayRepositoryImpl.saveAll(nodesToSave)
        logger.debug("Processed relay consensus descriptor for day $descriptorDay")
        return descriptorDay
    }

    /**
     * Use a server descriptor to save [NodeDetails] in the DB.
     */
    private fun processServerDescriptor(descriptor: ServerDescriptor): LocalDate {
        val descriptorDay = millisSinceEpochToLocalDate(descriptor.publishedMillis)
        val descriptorMonth = YearMonth.from(descriptorDay).toString()
        val existingNode =
            nodeDetailsRepository.getByMonthAndFingerprint(descriptorMonth, descriptor.fingerprint)
        if (existingNode == null || existingNode.day < descriptorDay) {
            nodeDetailsRepository.save(
                NodeDetails(
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
     * Confirms family structure and sets [NodeDetails.familyId] for the requested [months].
     * Afterwards the foreign keys of [GeoRelay] are updated.
     */
    private fun createNodeFamilies(months: Set<String>? = null) {
        val requestingNodes =
            if (months != null) nodeDetailsRepository.getAllByMonthInAndFamilyEntriesNotNull(months)
            else nodeDetailsRepository.getAllByFamilyEntriesNotNull()
        requestingNodes.forEach { requestingNode ->
            val confirmedFamilyNodes = mutableListOf<NodeDetails>()
            val month = requestingNode.month
            requestingNode.familyEntries!!.commaSeparatedToList().forEach {
                try {
                    confirmedFamilyNodes.add(confirmFamilyMember(requestingNode, it, month))
                } catch (exception: Exception) {
                }
            }
            saveNodeFamily(requestingNode, confirmedFamilyNodes)
        }
        logger.info("Updating all geo relay foreign ids")
        geoRelayRepositoryImpl.updateForeignIds()
        logger.info("Finished updating all geo relay foreign ids")
    }

    /**
     * Save a new family of nodes by updating their [NodeDetails.familyId]
     */
    private fun saveNodeFamily(
        requestingNode: NodeDetails,
        confirmedFamilyMembers: MutableList<NodeDetails>,
    ) {
        if (confirmedFamilyMembers.size > 0) {
            val newFamilyId = dbSequenceIncrementer.nextLongValue()
            confirmedFamilyMembers.add(requestingNode)
            confirmedFamilyMembers.forEach {
                it.familyId = newFamilyId
            }
            nodeDetailsRepository.saveAll(confirmedFamilyMembers)
        }
    }

    /**
     * Extract the fingerprint of a [allegedFamilyMemberId] for a [requestingNode] in a given [month]
     */
    private fun confirmFamilyMember(
        requestingNode: NodeDetails,
        allegedFamilyMemberId: String,
        month: String,
    ): NodeDetails {
        val fingerprintRegex = Regex("^\\$[A-F0-9]{40}.*")
        val nicknameRegex = Regex("^[a-zA-Z0-9]{1,19}$")
        when {
            fingerprintRegex.matches(allegedFamilyMemberId) -> {
                val allegedFamilyMember = nodeDetailsRepository.getByMonthAndFingerprint(
                    month,
                    allegedFamilyMemberId.substring(1, 40),
                )
                if (isNodeMemberOfFamily(requestingNode, allegedFamilyMember)) {
                    return allegedFamilyMember!!
                }
            }
            nicknameRegex.matches(allegedFamilyMemberId) -> {
                val allegedFamilyMembers =
                    nodeDetailsRepository.getAllByMonthAndNickname(month, allegedFamilyMemberId)
                val confirmedFamilyMember =
                    allegedFamilyMembers.firstOrNull { isNodeMemberOfFamily(requestingNode, it) }
                if (confirmedFamilyMember != null) {
                    return confirmedFamilyMember
                }
            }
            else -> throw Exception("Format of new family member $allegedFamilyMemberId for requestingNode ${requestingNode.fingerprint} not supported!")
        }
        throw Exception("New family member $allegedFamilyMemberId for requestingNode ${requestingNode.fingerprint} rejected!")
    }

    /**
     * Determines if the [requestingNode] shares a family with the [allegedFamilyMember].
     */
    private fun isNodeMemberOfFamily(
        requestingNode: NodeDetails,
        allegedFamilyMember: NodeDetails?,
    ) = allegedFamilyMember?.familyEntries?.commaSeparatedToList()?.any {
        it == "$" + requestingNode.fingerprint || it == requestingNode.nickname
    } ?: false
}

