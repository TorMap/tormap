package org.tormap.service

import org.springframework.jdbc.support.incrementer.PostgresSequenceMaxValueIncrementer
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import org.tormap.database.entity.RelayDetails
import org.tormap.database.repository.RelayDetailsRepositoryImpl
import org.tormap.util.computeRelayFamilies
import org.tormap.util.logger
import java.util.concurrent.CompletableFuture
import javax.sql.DataSource

/**
 * This service deals with [RelayDetails] entities
 */
@Service
class RelayDetailsUpdateService(
    private val relayDetailsRepositoryImpl: RelayDetailsRepositoryImpl,
    private val ipLookupService: IpLookupService,
    private val cacheService: CacheService,
    private val coalesceService: CoalesceService,
    private val transactionTemplate: TransactionTemplate,
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
        logger.info("Batch updating ASs for months: {}", monthsWithRelaysMissingAutonomousSystem.joinToString(", "))
        lookupMissingAutonomousSystems(monthsWithRelaysMissingAutonomousSystem)
    }

    fun lookupMissingAutonomousSystems(months: Set<String>) {
        months.forEach { month ->
            coalesceService.submitAsync("lookupMissingAutonomousSystems-$month") {
                logger.info("... Updating ASs for month: {}", month)
                var changedRelaysCount = 0
                val relaysWithoutAutonomousSystem =
                    relayDetailsRepositoryImpl.findAllByMonthEqualsAndAutonomousSystemNumberNull(month)
                relaysWithoutAutonomousSystem.forEach { relay ->
                    if (relay.lookupAndSetAutonomousSystem()) {
                        changedRelaysCount++
                    }
                }
                relayDetailsRepositoryImpl.saveAllAndFlush(relaysWithoutAutonomousSystem)
                logger.info(
                    "Determined the AS of $changedRelaysCount / ${relaysWithoutAutonomousSystem.size} relays for month $month",
                )
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
            this.autonomousSystemName = autonomousSystem.autonomousSystemOrganization()
            this.autonomousSystemNumber = autonomousSystem.autonomousSystemNumber().toInt()
            return true
        }
        return false
    }

    /**
     * Updates [RelayDetails.familyId] for all entities
     */
    fun computeAllMissingFamiliesAndEvictCache() {
        val monthFamilyMemberCount =
            relayDetailsRepositoryImpl.findDistinctMonthFamilyMemberCount().filter { it.count == 0L }
        val monthsToProcess = monthFamilyMemberCount.map { it.month }.toSet()
        // The families are computed asynchronously, evicting earlier would let requests cache the old families again
        computeFamilies(monthsToProcess).whenComplete { _, _ ->
            cacheService.evictRelayLocationsPerDay(monthsToProcess)
        }
    }

    /**
     * Updates [RelayDetails.familyId] for all entities of the requested [months] asynchronously.
     * @return a future that completes once the families of all [months] are computed (also if some failed)
     */
    fun computeFamilies(months: Set<String>): CompletableFuture<Void> {
        logger.info("... Updating relay families for months: {}", months.joinToString(", "))
        val futures = months.map { month ->
            coalesceService.submitAsync("computeFamilies-$month") {
                transactionTemplate.executeWithoutResult {
                    computeFamiliesForMonth(month)
                }
            }
                // Failures are logged by the CoalesceService, a failed month must not hide the others
                .exceptionally { null }
        }
        return CompletableFuture.allOf(*futures.toTypedArray())
    }

    /**
     * Updates [RelayDetails.familyId] of all entities for the given [month].
     */
    private fun computeFamiliesForMonth(month: String) {
        val requestingRelays = relayDetailsRepositoryImpl.findAllWithFamilyDeclarationByMonth(month)
        val result = computeRelayFamilies(requestingRelays) { relay, familyEntry ->
            logger.debug(
                "Format of alleged family member $familyEntry for requestingRelay ${relay.fingerprint} not supported!",
            )
        }
        relayDetailsRepositoryImpl.clearFamiliesFromMonth(month)
        saveFamilies(result.families)
        val totalFamilyConnectionCount = result.confirmedConnectionCount + result.rejectedConnectionCount
        logger.info(
            "Finished families for month $month. Rejected ${result.rejectedConnectionCount} / $totalFamilyConnectionCount family line connections. ${result.relaysWithFamilyKeyCount} relays have family certificates. Found ${result.families.size} different families.",
        )
    }

    /**
     * Save [families] of [RelayDetails] by updating their [RelayDetails.familyId].
     * Flushes once, because each flush checks all relays of the month for changes.
     */
    private fun saveFamilies(families: List<List<RelayDetails>>) {
        families.forEach { family ->
            val familyId = dbSequenceIncrementer.nextLongValue()
            family.forEach { it.familyId = familyId }
        }
        relayDetailsRepositoryImpl.saveAllAndFlush(families.flatten())
    }
}
