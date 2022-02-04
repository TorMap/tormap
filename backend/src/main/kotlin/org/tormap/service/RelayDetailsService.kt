package org.tormap.service

import org.springframework.jdbc.support.incrementer.H2SequenceMaxValueIncrementer
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.tormap.adapter.controller.RelayLocationController
import org.tormap.commaSeparatedToList
import org.tormap.database.entity.RelayDetails
import org.tormap.database.repository.RelayDetailsRepositoryImpl
import org.tormap.logger
import javax.transaction.Transactional


/**
 * This service deals with [RelayDetails] entities
 */
@Service
class RelayDetailsService(
    private val relayDetailsRepositoryImpl: RelayDetailsRepositoryImpl,
    private val dbSequenceIncrementer: H2SequenceMaxValueIncrementer,
    private val ipLookupService: IpLookupService,
    private val relayLocationController: RelayLocationController,
) {
    private val logger = logger()

    /**
     * Updates [RelayDetails.familyId] for all entities and if desired can also [overwriteExistingFamilies].
     */
    fun updateAllFamilies(overwriteExistingFamilies: Boolean) {
        var monthFamilyMemberCount = relayDetailsRepositoryImpl.findDistinctMonthFamilyMemberCount()
        if (!overwriteExistingFamilies) {
            monthFamilyMemberCount = monthFamilyMemberCount.filter { it.count == 0L }
        }
        updateFamilies(monthFamilyMemberCount.map { it.month }.toSet())
    }


    /**
     * Updates [RelayDetails.familyId] for all entities of the requested [months].
     */
    fun updateFamilies(months: Set<String>) {
        try {
            logger.info("... Updating relay families for months: ${months.joinToString(", ")}")
            relayDetailsRepositoryImpl.flush()
            months.forEach { month ->
                try {
                    updateFamiliesForMonth(month)
                    relayLocationController.cacheDaysOfMonth(month)
                } catch (exception: Exception) {
                    logger.error("Could not update relay families for month $month! ${exception.message}")
                }
            }
            logger.info("Finished updating relay families")
        } catch (exception: Exception) {
            logger.error("Could not update relay families! ${exception.message}")
        }
    }

    /**
     * Updates [RelayDetails.autonomousSystemName] and [RelayDetails.autonomousSystemNumber] for all [RelayDetails] missing this info.
     */
    @Async
    fun updateAutonomousSystems() {
        try {
            relayDetailsRepositoryImpl.flush()
            val monthsToProcess = relayDetailsRepositoryImpl.findDistinctMonthsAndAutonomousSystemNumberNull()
            logger.info("... Updating Autonomous Systems for months: ${monthsToProcess.joinToString(", ")}")
            monthsToProcess.forEach {
                var changedRelaysCount = 0
                val relaysWithoutAutonomousSystem =
                    relayDetailsRepositoryImpl.findAllByMonthEqualsAndAutonomousSystemNumberNull(it)
                relaysWithoutAutonomousSystem.forEach { relay ->
                    if (relay.updateAutonomousSystem()) {
                        changedRelaysCount++
                    }
                }
                relayDetailsRepositoryImpl.flush()
                if (changedRelaysCount > 0) {
                    logger.info("Finished Autonomous Systems for month $it. Updated $changedRelaysCount relays.")
                }
            }
            logger.info("Finished updating Autonomous System")
        } catch (exception: Exception) {
            logger.error("Could not update Autonomous System! ${exception.message}")
        }
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
                this.autonomousSystemNumber = autonomousSystem.autonomousSystemNumber.toInt()
                relayDetailsRepositoryImpl.save(this)
                return true
            }
        }
        return false
    }

    /**
     * Updates [RelayDetails.familyId] of all entities for the given [month].
     */
    private fun updateFamiliesForMonth(month: String) {
        var confirmedFamilyConnectionCount = 0
        var rejectedFamilyConnectionCount = 0
        val families = mutableListOf<Set<RelayDetails>>()
        val requestingRelays =
            relayDetailsRepositoryImpl.findAllByMonthEqualsAndFamilyEntriesNotNull(month)
        requestingRelays.forEach { requestingRelay ->
            requestingRelay.familyEntries!!.commaSeparatedToList().forEach { familyEntry ->
                try {
                    val newConfirmedMember =
                        requestingRelay.confirmFamilyMember(familyEntry, requestingRelays)
                    if (newConfirmedMember != null && requestingRelay != newConfirmedMember) {
                        families.addFamilyMember(requestingRelay, newConfirmedMember)
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
        families.saveToDatabase()
        val totalFamilyConnectionCount = confirmedFamilyConnectionCount + rejectedFamilyConnectionCount
        logger.info("Finished families for month $month. Rejected $rejectedFamilyConnectionCount / $totalFamilyConnectionCount connections. Found ${families.size} different families.")
    }

    /**
     * Check to which family the [requestingRelay] and the [newMember] belong
     */
    private fun MutableList<Set<RelayDetails>>.addFamilyMember(
        requestingRelay: RelayDetails,
        newMember: RelayDetails
    ) {
        val requestingRelayIndex = this.indexOfFirst { it.contains(requestingRelay) }
        val newMemberIndex = this.indexOfFirst { it.contains(newMember) }

        if (
            requestingRelayIndex >= 0
            && newMemberIndex >= 0
            && requestingRelayIndex != newMemberIndex
        ) {
            this[requestingRelayIndex] = this[requestingRelayIndex].plus(this[newMemberIndex])
            this.removeAt(newMemberIndex)
        } else if (requestingRelayIndex >= 0) {
            this[requestingRelayIndex] = this[requestingRelayIndex].plus(newMember)
        } else if (newMemberIndex >= 0) {
            this[newMemberIndex] = this[newMemberIndex].plus(requestingRelay)
        } else {
            this.add(setOf(requestingRelay, newMember))
        }
    }

    /**
     * Save families of [RelayDetails] by updating their [RelayDetails.familyId]
     */
    @Transactional
    fun List<Set<RelayDetails>>.saveToDatabase() {
        this.forEach { family ->
            val familyId = dbSequenceIncrementer.nextLongValue()
            family.forEach { it.familyId = familyId }
            relayDetailsRepositoryImpl.saveAllAndFlush(family)
        }
    }

    /**
     * Extract the fingerprint of a [allegedFamilyMemberId] and check if it is a family member of this [RelayDetails]
     */
    private fun RelayDetails.confirmFamilyMember(
        allegedFamilyMemberId: String,
        relaysWithFamilyEntries: List<RelayDetails>,
    ): RelayDetails? {
        when {
            familyEntryFingerprintRegex.matches(allegedFamilyMemberId) -> {
                val allegedFamilyMemberFingerprint = extractFingerprintFromFamilyEntry(allegedFamilyMemberId)
                val allegedFamilyMember =
                    relaysWithFamilyEntries.find { it.fingerprint == allegedFamilyMemberFingerprint }
                if (this.isMemberOfFamily(allegedFamilyMember)) {
                    return allegedFamilyMember
                }
            }
            familyEntryNicknameRegex.matches(allegedFamilyMemberId) -> {
                relaysWithFamilyEntries.filter { it.nickname == allegedFamilyMemberId }
                    .firstOrNull { this.isMemberOfFamily(it) }
            }
            else -> throw Exception("Format of alleged family member $allegedFamilyMemberId for requestingRelay ${this.fingerprint} not supported!")
        }
        return null
    }

    /**
     * Determines if this [RelayDetails] shares a family with the [allegedFamilyMember].
     */
    private fun RelayDetails.isMemberOfFamily(
        allegedFamilyMember: RelayDetails?
    ) = allegedFamilyMember?.familyEntries?.commaSeparatedToList()?.any {
        when {
            familyEntryFingerprintRegex.matches(it) -> {
                this.fingerprint == extractFingerprintFromFamilyEntry(it)
            }
            familyEntryNicknameRegex.matches(it) -> {
                this.nickname == it
            }
            else -> false
        }
    } ?: false

    private fun extractFingerprintFromFamilyEntry(familyEntry: String) = familyEntry.substring(1, 41)

    private val familyEntryFingerprintRegex = Regex("^\\$[A-F0-9]{40}.*$")
    private val familyEntryNicknameRegex = Regex("^[a-zA-Z0-9]{1,19}$")
}
