package org.tormap.service

import org.springframework.jdbc.support.incrementer.H2SequenceMaxValueIncrementer
import org.springframework.stereotype.Service
import org.tormap.commaSeparatedToList
import org.tormap.database.entity.NodeDetails
import org.tormap.database.repository.NodeDetailsRepository
import org.tormap.logger


/**
 * This service deals with [NodeDetails] entities
 */
@Service
class NodeDetailsService(
    val nodeDetailsRepository: NodeDetailsRepository,
    val dbSequenceIncrementer: H2SequenceMaxValueIncrementer,
) {
    val logger = logger()

    /**
     * Updates [NodeDetails.familyId] for all entities of the requested [months].
     */
    fun updateNodeFamilies(months: Set<String>) {
        try {
            logger.info("Updating node families")
            months.forEach {
                val requestingNodes = nodeDetailsRepository.getAllByMonthEqualsAndFamilyEntriesNotNull(it)
                requestingNodes.forEach { requestingNode ->
                    val confirmedFamilyNodes = mutableListOf<NodeDetails>()
                    val month = requestingNode.month
                    requestingNode.familyEntries!!.commaSeparatedToList().forEach {familyEntry ->
                        try {
                            confirmedFamilyNodes.add(confirmFamilyMember(requestingNode, familyEntry, month))
                        } catch (exception: Exception) {
                        }
                    }
                    saveNodeFamily(requestingNode, confirmedFamilyNodes)
                }
            }
            logger.info("Finished updating node families")
        } catch (exception: Exception) {
            logger.error("Could not update node families. ${exception.message}")
        }

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
                val allegedFamilyMember = nodeDetailsRepository.findByMonthAndFingerprintAndFamilyEntriesNotNull(
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

