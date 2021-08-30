package org.tormap.service

import org.springframework.jdbc.support.incrementer.H2SequenceMaxValueIncrementer
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.tormap.calculateIPv4NumberRepresentation
import org.tormap.commaSeparatedToList
import org.tormap.database.entity.NodeDetails
import org.tormap.database.repository.AutonomousSystemRepositoryImpl
import org.tormap.database.repository.NodeDetailsRepositoryImpl
import org.tormap.logger


/**
 * This service deals with [NodeDetails] entities
 */
@Service
class NodeDetailsService(
    val nodeDetailsRepositoryImpl: NodeDetailsRepositoryImpl,
    val dbSequenceIncrementer: H2SequenceMaxValueIncrementer,
    val autonomousSystemRepositoryImpl: AutonomousSystemRepositoryImpl,
) {
    val logger = logger()

    /**
     * Updates [NodeDetails.familyId] for all entities of the requested [months].
     */
    fun updateNodeFamilies(months: Set<String>? = null, overwriteExistingFamilies: Boolean = false) {
        try {
            val monthsToProcess = when {
                months != null -> months
                else -> {
                    var monthFamilyMemberCount = nodeDetailsRepositoryImpl.findDistinctMonthFamilyMemberCount()
                    if (!overwriteExistingFamilies) {
                        monthFamilyMemberCount = monthFamilyMemberCount.filter { it.count > 0 }
                    }
                    monthFamilyMemberCount.map { it.month }
                }
            }
            logger.info("Updating node families for months: ${monthsToProcess.joinToString(", ")}")
            monthsToProcess.forEach { month ->
                nodeDetailsRepositoryImpl.clearFamiliesFromMonth(month)
                var confirmedFamilyConnectionCount = 0
                var rejectedFamilyConnectionCount = 0
                val families = mutableListOf<Set<NodeDetails>>()
                val requestingFamilyNodes = nodeDetailsRepositoryImpl.findAllByMonthEqualsAndFamilyEntriesNotNull(month)
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
                saveFamilies(families)
                val totalFamilyConnectionCount = confirmedFamilyConnectionCount + rejectedFamilyConnectionCount
                logger.info("For month $month this many family connections were rejected: $rejectedFamilyConnectionCount / $totalFamilyConnectionCount")
            }
            logger.info("Finished updating node families")
        } catch (exception: Exception) {
            logger.error("Could not update node families. ${exception.message}")
        }
    }

    /**
     * Updates [NodeDetails.autonomousSystemName] and [NodeDetails.autonomousSystemNumber] for all entities of the requested [months].
     */
    @Async
    fun updateAutonomousSystems(months: Set<String>? = null) {
        try {
            val monthsToProcess = months ?: nodeDetailsRepositoryImpl.findDistinctMonthsAndAutonomousSystemNumberNull()
            logger.info("Updating node Autonomous Systems for months: ${monthsToProcess.joinToString(", ")}")
            monthsToProcess.forEach {
                var changedNodesCount = 0
                val nodesWithoutAS = nodeDetailsRepositoryImpl.findAllByMonthEqualsAndAutonomousSystemNumberNull(it)
                nodesWithoutAS.forEach { node ->
                    if (node.updateAutonomousSystem()) {
                        changedNodesCount++
                    }
                }
                logger.info("For month $it this many node Autonomous Systems were changed: $changedNodesCount")
            }
            logger.info("Finished updating Autonomous System in NodeDetails")
        } catch (exception: Exception) {
            logger.error("Could not update Autonomous System in NodeDetails. ${exception.message}")
        }
    }

    /**
     * Trys to add an Autonomous System to [this]
     * @return true if node was changed
     */
    private fun NodeDetails.updateAutonomousSystem(): Boolean {
        if (this.address != null) {
            val autonomousSystem = try {
                this.addressNumber = this.addressNumber ?: calculateIPv4NumberRepresentation(this.address!!)
                autonomousSystemRepositoryImpl.findUsingIPv4(this.addressNumber!!)
            } catch (exception: Exception) {
                logger().warn("Could not search autonomousSystem for address ${this.address}")
                null
            }
            if (autonomousSystem != null) {
                this.autonomousSystemName = autonomousSystem.autonomousSystemName
                this.autonomousSystemNumber = autonomousSystem.autonomousSystemNumber
                nodeDetailsRepositoryImpl.save(this)
                return true
            }
        }
        return false
    }

    /**
     * Save a new family of nodes by updating their [NodeDetails.familyId]
     */
    private fun saveFamilies(
        families: List<Set<NodeDetails>>
    ) {
        families.forEach { family ->
            val familyId = dbSequenceIncrementer.nextLongValue()
            family.forEach { it.familyId = familyId }
            nodeDetailsRepositoryImpl.saveAll(family)
        }
    }

    /**
     * Extract the fingerprint of a [allegedFamilyMemberId] and check if it is a family member of the [requestingNode]
     */
    private fun confirmFamilyMember(
        requestingNode: NodeDetails,
        allegedFamilyMemberId: String,
        requestingFamilyNodes: List<NodeDetails>,
    ): NodeDetails? {
        val fingerprintRegex = Regex("^\\$[A-F0-9]{40}.*$")
        val nicknameRegex = Regex("^[a-zA-Z0-9]{1,19}$")
        when {
            fingerprintRegex.matches(allegedFamilyMemberId) -> {
                val allegedFamilyMember =
                    requestingFamilyNodes.find { it.fingerprint == allegedFamilyMemberId.substring(1, 41) }
                if (isNodeMemberOfFamily(requestingNode, allegedFamilyMember)) {
                    return allegedFamilyMember
                }
            }
            nicknameRegex.matches(allegedFamilyMemberId) -> {
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
        requestingNode: NodeDetails,
        allegedFamilyMember: NodeDetails?,
    ) = allegedFamilyMember?.familyEntries?.commaSeparatedToList()?.any {
        it == "$" + requestingNode.fingerprint || it == requestingNode.nickname
    } ?: false
}

