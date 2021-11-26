package org.tormap.service

import org.springframework.cache.CacheManager
import org.springframework.jdbc.support.incrementer.H2SequenceMaxValueIncrementer
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.tormap.adapter.controller.ArchiveDataController
import org.tormap.commaSeparatedToList
import org.tormap.database.entity.RelayDetails
import org.tormap.database.repository.RelayDetailsRepositoryImpl
import org.tormap.logger
import java.time.YearMonth
import javax.transaction.Transactional


/**
 * This service deals with [RelayDetails] entities
 */
@Service
class RelayDetailsService(
    private val relayDetailsRepositoryImpl: RelayDetailsRepositoryImpl,
    private val dbSequenceIncrementer: H2SequenceMaxValueIncrementer,
    private val ipLookupService: IpLookupService,
    private val cacheManager: CacheManager,
    private val archiveDataController: ArchiveDataController,
) {
    private val logger = logger()

    /**
     * Updates [RelayDetails.familyId] for all entities and if desired also [overwriteExistingFamilies].
     */
    fun updateAllNodeFamilies(overwriteExistingFamilies: Boolean) {
        var monthFamilyMemberCount = relayDetailsRepositoryImpl.findDistinctMonthFamilyMemberCount()
        if (!overwriteExistingFamilies) {
            monthFamilyMemberCount = monthFamilyMemberCount.filter { it.count == 0L }
        }
        updateNodeFamilies(monthFamilyMemberCount.map { it.month }.toSet())
    }


    /**
     * Updates [RelayDetails.familyId] for all entities of the requested [months].
     */
    fun updateNodeFamilies(months: Set<String>) {
        try {
            logger.info("Updating node families for months: ${months.joinToString(", ")}")
            months.forEach { month ->
                try {
                    updateNodeFamiliesForMonth(month)
                    updateGeoRelayDayCache(month)
                } catch (exception: Exception) {
                    logger.error("Could not update node families for month $month! ${exception.message}")
                }
            }
            logger.info("Finished updating node families")
        } catch (exception: Exception) {
            logger.error("Could not update node families! ${exception.message}")
        }
    }

    @Async
    fun updateGeoRelayDayCache(month: String) {
        val yearMonth = YearMonth.parse(month)
        yearMonth.atDay(1).datesUntil(yearMonth.plusMonths(1).atDay(1)).forEach {
            val day = it.toString()
            cacheManager.getCache("geo-relay-day")?.evict(day)
            archiveDataController.getGeoRelaysByDay(day)
        }
    }

    /**
     * Updates [RelayDetails.autonomousSystemName] and [RelayDetails.autonomousSystemNumber] for all entities of the requested [months].
     */
    @Async
    fun updateAutonomousSystems(months: Set<String>? = null) {
        try {
            val monthsToProcess = months ?: relayDetailsRepositoryImpl.findDistinctMonthsAndAutonomousSystemNumberNull()
            logger.info("Updating Autonomous Systems for months: ${monthsToProcess.joinToString(", ")}")
            monthsToProcess.forEach {
                var changedNodesCount = 0
                val nodesWithoutAS = relayDetailsRepositoryImpl.findAllByMonthEqualsAndAutonomousSystemNumberNull(it)
                nodesWithoutAS.forEach { node ->
                    if (node.updateAutonomousSystem()) {
                        changedNodesCount++
                    }
                }
                relayDetailsRepositoryImpl.flush()
                if (changedNodesCount > 0) {
                    logger.info("Finished Autonomous Systems for month $it. Updated $changedNodesCount nodes.")
                }
            }
            logger.info("Finished updating Autonomous System")
        } catch (exception: Exception) {
            logger.error("Could not update Autonomous System! ${exception.message}")
        }
    }

    /**
     * Updates [RelayDetails.familyId] for all entities of the requested [month].
     */
    private fun updateNodeFamiliesForMonth(month: String) {
        var confirmedFamilyConnectionCount = 0
        var rejectedFamilyConnectionCount = 0
        val families = mutableListOf<Set<RelayDetails>>()
        val requestingFamilyNodes =
            relayDetailsRepositoryImpl.findAllByMonthEqualsAndFamilyEntriesNotNull(month)
        requestingFamilyNodes.forEach { requestingNode ->
            requestingNode.familyEntries!!.commaSeparatedToList().forEach { familyEntry ->
                try {
                    val newConfirmedMember =
                        confirmFamilyMember(requestingNode, familyEntry, requestingFamilyNodes)
                    if (newConfirmedMember != null && requestingNode != newConfirmedMember) {
                        val existingFamilyRequestingNodeIndex =
                            families.indexOfFirst { it.contains(requestingNode) }
                        val existingFamilyNewConfirmedMemberIndex =
                            families.indexOfFirst { it.contains(newConfirmedMember) }

                        if (
                            existingFamilyRequestingNodeIndex >= 0
                            && existingFamilyNewConfirmedMemberIndex >= 0
                            && existingFamilyRequestingNodeIndex != existingFamilyNewConfirmedMemberIndex
                        ) {
                            families[existingFamilyRequestingNodeIndex] =
                                families[existingFamilyRequestingNodeIndex].plus(families[existingFamilyNewConfirmedMemberIndex])
                            families.removeAt(existingFamilyNewConfirmedMemberIndex)
                        } else if (existingFamilyRequestingNodeIndex >= 0) {
                            families[existingFamilyRequestingNodeIndex] =
                                families[existingFamilyRequestingNodeIndex].plus(newConfirmedMember)
                        } else if (existingFamilyNewConfirmedMemberIndex >= 0) {
                            families[existingFamilyNewConfirmedMemberIndex] =
                                families[existingFamilyNewConfirmedMemberIndex].plus(requestingNode)
                        } else {
                            families.add(setOf(requestingNode, newConfirmedMember))
                        }

                        confirmedFamilyConnectionCount++
                    } else {
                        rejectedFamilyConnectionCount++
                    }
                } catch (exception: Exception) {
                    logger.debug(exception.message)
                    rejectedFamilyConnectionCount++
                }
            }
        }
        relayDetailsRepositoryImpl.clearFamiliesFromMonth(month)
        saveFamilies(families)
        val totalFamilyConnectionCount = confirmedFamilyConnectionCount + rejectedFamilyConnectionCount
        logger.info("Finished families for month $month. Rejected $rejectedFamilyConnectionCount / $totalFamilyConnectionCount connections. Found ${families.size} different families.")
    }

    /**
     * Trys to add an Autonomous System to [this]
     * @return true if node was changed
     */
    private fun RelayDetails.updateAutonomousSystem(): Boolean {
        if (this.address != null) {
            val autonomousSystem = ipLookupService.lookupAutonomousSystem(this.address!!)
            if (autonomousSystem != null) {
                this.autonomousSystemName = autonomousSystem.autonomousSystemOrganization
                this.autonomousSystemNumber = autonomousSystem.autonomousSystemNumber
                relayDetailsRepositoryImpl.save(this)
                return true
            }
        }
        return false
    }

    /**
     * Save a new family of nodes by updating their [RelayDetails.familyId]
     */
    @Transactional
    fun saveFamilies(
        families: List<Set<RelayDetails>>
    ) {
        families.forEach { family ->
            val familyId = dbSequenceIncrementer.nextLongValue()
            family.forEach { it.familyId = familyId }
            relayDetailsRepositoryImpl.saveAll(family)
        }
        relayDetailsRepositoryImpl.flush()
    }

    /**
     * Extract the fingerprint of a [allegedFamilyMemberId] and check if it is a family member of the [requestingNode]
     */
    private fun confirmFamilyMember(
        requestingNode: RelayDetails,
        allegedFamilyMemberId: String,
        requestingFamilyNodes: List<RelayDetails>,
    ): RelayDetails? {
        when {
            familyEntryFingerprintRegex.matches(allegedFamilyMemberId) -> {
                val allegedFamilyMemberFingerprint = extractFingerprintFromFamilyEntry(allegedFamilyMemberId)
                val allegedFamilyMember =
                    requestingFamilyNodes.find { it.fingerprint == allegedFamilyMemberFingerprint }
                if (isNodeMemberOfFamily(requestingNode, allegedFamilyMember)) {
                    return allegedFamilyMember
                }
            }
            familyEntryNicknameRegex.matches(allegedFamilyMemberId) -> {
                requestingFamilyNodes.filter { it.nickname == allegedFamilyMemberId }
                    .firstOrNull { isNodeMemberOfFamily(requestingNode, it) }
            }
            else -> throw Exception("Format of new family member $allegedFamilyMemberId for requestingNode ${requestingNode.fingerprint} not supported!")
        }
        return null
    }

    /**
     * Determines if the [requestingNode] shares a family with the [allegedFamilyMember].
     */
    private fun isNodeMemberOfFamily(
        requestingNode: RelayDetails,
        allegedFamilyMember: RelayDetails?,
    ) = allegedFamilyMember?.familyEntries?.commaSeparatedToList()?.any {
        when {
            familyEntryFingerprintRegex.matches(it) -> {
                requestingNode.fingerprint == extractFingerprintFromFamilyEntry(it)
            }
            familyEntryNicknameRegex.matches(it) -> {
                requestingNode.nickname == it
            }
            else -> throw Exception("Format of new family member ${allegedFamilyMember.id} for requestingNode ${requestingNode.fingerprint} not supported!")
        }
    } ?: false

    private fun extractFingerprintFromFamilyEntry(familyEntry: String) = familyEntry.substring(1, 41)

    private val familyEntryFingerprintRegex = Regex("^\\$[A-F0-9]{40}.*$")
    private val familyEntryNicknameRegex = Regex("^[a-zA-Z0-9]{1,19}$")
}

