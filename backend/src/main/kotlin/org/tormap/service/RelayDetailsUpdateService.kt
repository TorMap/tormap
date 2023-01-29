package org.tormap.service

import mu.KotlinLogging
import org.springframework.jdbc.support.incrementer.PostgresSequenceMaxValueIncrementer
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.tormap.adapter.controller.RelayLocationController
import org.tormap.database.entity.RelayDetails
import org.tormap.database.repository.RelayDetailsRepository
import org.tormap.util.addFamilyMember
import org.tormap.util.commaSeparatedToList
import org.tormap.util.getFamilyMember
import javax.sql.DataSource

// TODO: check why data source is used directly here
/**
 * This service deals with [RelayDetails] entities
 */
@Service
class RelayDetailsUpdateService(
    private val relayDetailsRepository: RelayDetailsRepository,
    private val ipLookupService: IpLookupService,
    private val relayLocationController: RelayLocationController,
    dataSource: DataSource,
) {
    private val logger = KotlinLogging.logger { }

    private val dbSequenceIncrementer = PostgresSequenceMaxValueIncrementer(dataSource, "relay_sequence")

    /**
     * Updates [RelayDetails.autonomousSystemName] and [RelayDetails.autonomousSystemNumber] for all [RelayDetails] missing this info.
     */
    @Async
    fun updateAutonomousSystems() {
        try {
            val monthsToProcess = relayDetailsRepository.findDistinctMonthsAndAutonomousSystemNumberNull()
            logger.info { "... Updating Autonomous Systems for months: ${monthsToProcess.joinToString(", ")}" }
            for (month in monthsToProcess) {
                var changedRelaysCount = 0
                val relaysWithoutAutonomousSystem =
                    relayDetailsRepository.findAllByMonthAndAutonomousSystemNumberNull(month)
                relaysWithoutAutonomousSystem.forEach { relay ->
                    if (relay.updateAutonomousSystem()) {
                        changedRelaysCount++
                    }
                }
                if (changedRelaysCount > 0) {
                    logger.info { "Finished Autonomous Systems for month $month. Updated $changedRelaysCount relays" }
                }
            }
            logger.info { "Finished updating Autonomous System" }
        } catch (exception: Exception) {
            logger.error(exception) { "Could not update Autonomous System!" }
        }
    }

    /**
     * Trys to add an Autonomous System to [this]
     * @return true if node was changed
     */
    private fun RelayDetails.updateAutonomousSystem(): Boolean {
        val autonomousSystem = ipLookupService.lookupAutonomousSystem(this.address)
        if (autonomousSystem != null) {
            this.autonomousSystemName = autonomousSystem.autonomousSystemOrganization
            this.autonomousSystemNumber = autonomousSystem.autonomousSystemNumber.toInt()
            relayDetailsRepository.save(this)
            return true
        }
        return false
    }

    /**
     * Updates [RelayDetails.familyId] for all entities and if desired can also [overwriteExistingFamilies].
     */
    fun updateAllFamilies(overwriteExistingFamilies: Boolean) {
        var monthFamilyMemberCount = relayDetailsRepository.findDistinctMonthFamilyMemberCount()
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
            logger.info { "... Updating relay families for months: ${months.joinToString(", ")}" }
            for (month in months) {
                try {
                    updateFamiliesForMonth(month)
                } catch (exception: Exception) {
                    logger.error("Could not update relay families for month $month! ${exception.message}")
                }
            }
            logger.info { "Finished updating relay families" }
        } catch (exception: Exception) {
            logger.error(exception) { "Could not update relay families!" }
        }
    }

    /**
     * Updates [RelayDetails.familyId] of all entities for the given [month].
     */
    private fun updateFamiliesForMonth(month: String) {
        var confirmedFamilyConnectionCount = 0
        var rejectedFamilyConnectionCount = 0
        val families = mutableListOf<Set<RelayDetails>>()
        val requestingRelays =
            relayDetailsRepository.findAllByMonthAndFamilyEntriesNotNull(month)
        requestingRelays.forEach { requestingRelay ->
            requestingRelay.familyEntries!!.commaSeparatedToList().forEach { familyEntry ->
                try {
                    val newConfirmedMember =
                        requestingRelay.getFamilyMember(familyEntry, requestingRelays)
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
        relayDetailsRepository.clearFamiliesFromMonth(month)
        families.saveToDatabase()
        val totalFamilyConnectionCount = confirmedFamilyConnectionCount + rejectedFamilyConnectionCount
        logger.info {
            "Finished families for month $month. Rejected $rejectedFamilyConnectionCount / " +
                "$totalFamilyConnectionCount connections. Found ${families.size} different families."
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
            relayDetailsRepository.saveAll(family)
        }
    }
}
