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
    fun updateNodeFamilies(months: Set<String>? = null) {
        try {
            val monthsToProcess = months ?: nodeDetailsRepositoryImpl.findDistinctMonths()
            logger.info("Updating node families for months: ${monthsToProcess.joinToString(", ")}")
            monthsToProcess.forEach {
                var confirmedFamilyConnectionCount = 0
                var rejectedFamilyConnectionCount = 0
                var membersOfProcessedFamilies = mutableMapOf<String, Long>()
                val requestingNodes = nodeDetailsRepositoryImpl.findAllByMonthEqualsAndFamilyEntriesNotNull(it)
                requestingNodes.forEach { requestingNode ->
                    val confirmedFamilyNodes = mutableListOf<NodeDetails>()
                    val month = requestingNode.month
                    var familyId: Long? = null
                    requestingNode.familyEntries!!.commaSeparatedToList().forEach { familyEntry ->
                        try {
                            val newConfirmedMember = confirmFamilyMember(requestingNode, familyEntry, month)
                            if (newConfirmedMember != null) {
                                if (!membersOfProcessedFamilies.containsKey(newConfirmedMember.fingerprint)) {
                                    confirmedFamilyNodes.add(newConfirmedMember)
                                }
                                familyId = membersOfProcessedFamilies[requestingNode.fingerprint]
                                    ?: membersOfProcessedFamilies[newConfirmedMember.fingerprint]
                                confirmedFamilyConnectionCount++
                            } else {
                                rejectedFamilyConnectionCount++
                            }
                        } catch (exception: Exception) {
                            logger.debug(exception.message)
                            rejectedFamilyConnectionCount++
                        }
                    }
                    membersOfProcessedFamilies =
                        saveNodeFamily(requestingNode, confirmedFamilyNodes, membersOfProcessedFamilies, familyId)
                }
                val totalFamilyConnectionCount = confirmedFamilyConnectionCount + rejectedFamilyConnectionCount
                logger.info("For month $it this many family connections were rejected: $rejectedFamilyConnectionCount / $totalFamilyConnectionCount")
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
    private fun saveNodeFamily(
        requestingNode: NodeDetails,
        confirmedFamilyMembers: MutableList<NodeDetails>,
        membersOfOtherFamilies: MutableMap<String, Long>,
        familyId: Long?,
    ): MutableMap<String, Long> {
        if (confirmedFamilyMembers.isNotEmpty() || familyId != null) {
            if (!membersOfOtherFamilies.containsKey(requestingNode.fingerprint)) {
                confirmedFamilyMembers.add(requestingNode)
            }
            val newFamilyId = familyId ?: dbSequenceIncrementer.nextLongValue()
            confirmedFamilyMembers.forEach {
                it.familyId = newFamilyId
                membersOfOtherFamilies[it.fingerprint] = newFamilyId
            }
            nodeDetailsRepositoryImpl.saveAll(confirmedFamilyMembers)
        }
        return membersOfOtherFamilies
    }

    /**
     * Extract the fingerprint of a [allegedFamilyMemberId] for a [requestingNode] in a given [month]
     */
    private fun confirmFamilyMember(
        requestingNode: NodeDetails,
        allegedFamilyMemberId: String,
        month: String,
    ): NodeDetails? {
        val fingerprintRegex = Regex("^\\$[A-F0-9]{40}.*$")
        val nicknameRegex = Regex("^[a-zA-Z0-9]{1,19}$")
        when {
            fingerprintRegex.matches(allegedFamilyMemberId) -> {
                val allegedFamilyMember = nodeDetailsRepositoryImpl.findByMonthAndFingerprintAndFamilyEntriesNotNull(
                    month,
                    allegedFamilyMemberId.substring(1, 41),
                )
                if (isNodeMemberOfFamily(requestingNode, allegedFamilyMember)) {
                    return allegedFamilyMember!!
                }
            }
            nicknameRegex.matches(allegedFamilyMemberId) -> {
                val allegedFamilyMembers =
                    nodeDetailsRepositoryImpl.findAllByMonthAndNickname(month, allegedFamilyMemberId)
                return allegedFamilyMembers.firstOrNull { isNodeMemberOfFamily(requestingNode, it) }
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

