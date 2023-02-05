package org.tormap.service

import mu.KotlinLogging
import org.springframework.jdbc.support.incrementer.PostgresSequenceMaxValueIncrementer
import org.springframework.stereotype.Service
import org.tormap.database.entity.RelayDetails
import org.tormap.database.repository.RelayDetailsRepository
import org.tormap.util.addFamilyMember
import org.tormap.util.getFamilyMember
import javax.sql.DataSource

/**
 * This service deals with [RelayDetails] entities.
 */
@Service
class RelayDetailsUpdateService(
    private val relayDetailsRepository: RelayDetailsRepository,
    private val ipLookupService: IpLookupService,
    dataSource: DataSource
) {

    private val logger = KotlinLogging.logger { }

    private val dbSequenceIncrementer = PostgresSequenceMaxValueIncrementer(dataSource, "relay_family_sequence")

    /**
     * Updates [RelayDetails.autonomousSystemName] and [RelayDetails.autonomousSystemNumber] for all [RelayDetails] missing this info.
     */
    fun updateAutonomousSystems() {
        val monthsToProcess = relayDetailsRepository.findDistinctMonthsWithAutonomousSystemNumberNull()
        logger.info { "... Updating Autonomous Systems for months: ${monthsToProcess.joinToString()}" }

        for (month in monthsToProcess) {
            val relaysWithoutAS = relayDetailsRepository.findAllByMonthAndAutonomousSystemNumberNull(month)
            val updatedRelayDetails = relaysWithoutAS.filter { it.updateAutonomousSystem() }

            if (updatedRelayDetails.isNotEmpty()) {
                relayDetailsRepository.saveAll(updatedRelayDetails)
                logger.info { "Finished Autonomous Systems for month $month. Updated $updatedRelayDetails relays" }
            }
        }

        logger.info { "Finished updating of Autonomous System" }
    }

    /**
     * Trys to add an Autonomous System to [this].
     * @return true if details were updated
     */
    private fun RelayDetails.updateAutonomousSystem(): Boolean {
        val autonomousSystem = ipLookupService.lookupAutonomousSystem(this.address)

        return if (autonomousSystem == null) false
        else {
            this.autonomousSystemName = autonomousSystem.autonomousSystemOrganization
            this.autonomousSystemNumber = autonomousSystem.autonomousSystemNumber.toInt()
            true
        }
    }

    /**
     * Updates [RelayDetails.familyId] for all entities and if desired can also [overwriteExistingFamilies].
     */
    fun updateAllFamilies(overwriteExistingFamilies: Boolean) {
        val month = if (overwriteExistingFamilies) {
            relayDetailsRepository.findDistinctMonthFamilyMemberCount()
        } else {
            relayDetailsRepository.findDistinctMonthFamilyMemberCount(0)
        }

        updateFamilies(month)
    }

    /**
     * Updates [RelayDetails.familyId] for all entities of the requested [months].
     */
    fun updateFamilies(months: List<String>) {
        logger.info { "... Updating relay families for months: ${months.joinToString()}" }
        months.forEach { month -> updateFamiliesForMonth(month) }
        logger.info { "Finished updating relay families" }
    }

    private data class FamilyGroupingResult(
        val confirmedFamilyConnectionCount: Int,
        val rejectedFamilyConnectionCount: Int,
        val families: List<List<RelayDetails>>
    )

    private fun groupFamilies(relayDetails: List<RelayDetails>): FamilyGroupingResult {
        var confirmedFamilyConnectionCount = 0
        var rejectedFamilyConnectionCount = 0
        val families = mutableListOf<List<RelayDetails>>()

        for (relay in relayDetails) {
            val familyEntries = relay.familyEntries?.split(", ") ?: emptyList()
            for (familyEntry in familyEntries) {
                val newConfirmedMember = relay.getFamilyMember(familyEntry, relayDetails)

                if (newConfirmedMember != null && relay != newConfirmedMember) {
                    families.addFamilyMember(relay, newConfirmedMember)
                    confirmedFamilyConnectionCount++
                } else {
                    rejectedFamilyConnectionCount++
                }
            }
        }

        return FamilyGroupingResult(confirmedFamilyConnectionCount, rejectedFamilyConnectionCount, families)
    }

    private fun updateFamilies(month: String, families: List<List<RelayDetails>>) {
        relayDetailsRepository.clearFamiliesFromMonth(month)

        for (relayDetailsFamilyGroup in families) {
            val familyId = dbSequenceIncrementer.nextLongValue()
            relayDetailsFamilyGroup.forEach { it.familyId = familyId }
            relayDetailsRepository.saveAll(relayDetailsFamilyGroup)
        }
    }

    /**
     * Updates [RelayDetails.familyId] of all entities for the given [month].
     */
    private fun updateFamiliesForMonth(month: String) {
        val relayDetails = relayDetailsRepository.findAllByMonthAndFamilyEntriesNotNull(month)
        val (confirmedFamilyConnectionCount, rejectedFamilyConnectionCount, families) = groupFamilies(relayDetails)

        updateFamilies(month, families)

        val totalFamilyConnectionCount = confirmedFamilyConnectionCount + rejectedFamilyConnectionCount
        logger.info {
            "Finished families for month $month. Rejected $rejectedFamilyConnectionCount / " +
                "$totalFamilyConnectionCount connections. Found ${families.size} different families."
        }
    }
}
