package org.tormap.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.jdbc.support.incrementer.PostgresSequenceMaxValueIncrementer
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.tormap.adapter.controller.RelayLocationController
import org.tormap.database.entity.RelayDetails
import org.tormap.database.repository.RelayDetailsRepositoryImpl
import org.tormap.util.addFamilyMember
import org.tormap.util.commaSeparatedToList
import org.tormap.util.getFamilyMember
import javax.sql.DataSource

/**
 * This service deals with [RelayDetails] entities
 */
@Service
class RelayDetailsUpdateService(
    private val relayDetailsRepositoryImpl: RelayDetailsRepositoryImpl,
    private val ipLookupService: IpLookupService,
    private val relayLocationController: RelayLocationController,
    dataSource: DataSource,
) {
    private val logger = KotlinLogging.logger { }
    private val dbSequenceIncrementer = PostgresSequenceMaxValueIncrementer(dataSource, "hibernate_sequence")

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
        val autonomousSystem = ipLookupService.lookupAutonomousSystem(this.address)
        if (autonomousSystem != null) {
            this.autonomousSystemName = autonomousSystem.autonomousSystemOrganization
            this.autonomousSystemNumber = autonomousSystem.autonomousSystemNumber.toInt()
            relayDetailsRepositoryImpl.save(this)
            return true
        }
        return false
    }

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
        relayDetailsRepositoryImpl.clearFamiliesFromMonth(month)
        families.saveToDatabase()
        val totalFamilyConnectionCount = confirmedFamilyConnectionCount + rejectedFamilyConnectionCount
        logger.info("Finished families for month $month. Rejected $rejectedFamilyConnectionCount / $totalFamilyConnectionCount connections. Found ${families.size} different families.")
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
}
