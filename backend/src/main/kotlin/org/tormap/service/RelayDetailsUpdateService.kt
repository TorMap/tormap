package org.tormap.service

import org.springframework.jdbc.support.incrementer.PostgresSequenceMaxValueIncrementer
import org.springframework.stereotype.Service
import org.tormap.database.entity.RelayDetails
import org.tormap.database.repository.RelayDetailsRepositoryImpl
import org.tormap.util.addFamilyMember
import org.tormap.util.commaSeparatedToList
import org.tormap.util.getFamilyMember
import org.tormap.util.logger
import javax.sql.DataSource
import javax.transaction.Transactional


/**
 * This service deals with [RelayDetails] entities
 */
@Service
class RelayDetailsUpdateService(
    private val relayDetailsRepositoryImpl: RelayDetailsRepositoryImpl,
    private val ipLookupService: IpLookupService,
    dataSource: DataSource,
) {
    private val logger = logger()
    private val dbSequenceIncrementer = PostgresSequenceMaxValueIncrementer(dataSource, "hibernate_sequence")

    /**
     * Updates [RelayDetails.autonomousSystemName] and [RelayDetails.autonomousSystemNumber] for all [RelayDetails] missing this info.
     */
    fun lookupAllMissingAutonomousSystems() {
        val monthsWithRelaysMissingAutonomousSystem =
            relayDetailsRepositoryImpl.findDistinctMonthsAndAutonomousSystemNumberNull()
        logger.info("Batch updating ASs for months: ${monthsWithRelaysMissingAutonomousSystem.joinToString(", ")}")
        lookupMissingAutonomousSystems(monthsWithRelaysMissingAutonomousSystem)
        logger.info("Finished batch update of ASs")
    }

    fun lookupMissingAutonomousSystems(months: Set<String>) {
        months.forEach { month ->
            try {
                logger.info("... Updating ASs for month: $month")
                var changedRelaysCount = 0
                val relaysWithoutAutonomousSystem =
                    relayDetailsRepositoryImpl.findAllByMonthEqualsAndAutonomousSystemNumberNull(month)
                relaysWithoutAutonomousSystem.forEach { relay ->
                    if (relay.lookupAndSetAutonomousSystem()) {
                        changedRelaysCount++
                    }
                }
                relayDetailsRepositoryImpl.saveAllAndFlush(relaysWithoutAutonomousSystem)
                logger.info("Determined the AS of $changedRelaysCount / ${relaysWithoutAutonomousSystem.size} relays for month $month")
            } catch (exception: Exception) {
                logger.error("Could not update ASs for month $month! ${exception.message}")
            }
        }
    }

    /**
     * Trys to add Autonomous System info to [RelayDetails]
     * @return true if node was changed
     */
    private fun RelayDetails.lookupAndSetAutonomousSystem(): Boolean {
        val autonomousSystem = ipLookupService.lookupAutonomousSystem(this.address)
        if (autonomousSystem != null) {
            this.autonomousSystemName = autonomousSystem.autonomousSystemOrganization
            this.autonomousSystemNumber = autonomousSystem.autonomousSystemNumber.toInt()
            return true
        }
        return false
    }

    /**
     * Updates [RelayDetails.familyId] for all entities
     */
    fun computeAllMissingFamilies() {
        val monthFamilyMemberCount =
            relayDetailsRepositoryImpl.findDistinctMonthFamilyMemberCount().filter { it.count == 0L }
        computeFamilies(monthFamilyMemberCount.map { it.month }.toSet())
    }


    /**
     * Updates [RelayDetails.familyId] for all entities of the requested [months].
     */
    fun computeFamilies(months: Set<String>) {
        try {
            logger.info("... Updating relay families for months: ${months.joinToString(", ")}")
            months.forEach { month ->
                try {
                    computeFamiliesForMonth(month)
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
    private fun computeFamiliesForMonth(month: String) {
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
