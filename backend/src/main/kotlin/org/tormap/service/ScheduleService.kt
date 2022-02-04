package org.tormap.service

import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.tormap.config.value.DescriptorConfig
import org.tormap.config.value.ScheduleConfig
import org.tormap.database.entity.DescriptorType


/**
 * This scheduler sets reoccurring events to collect and process data about Tor relays
 * With the @Async class annotation all methods run in parallel on separate threads if available.
 */
@Service
@Async
class ScheduleService(
    private val descriptorConfig: DescriptorConfig,
    private val scheduleConfig: ScheduleConfig,
    private val torDescriptorService: TorDescriptorService,
    private val relayDetailsUpdateService: RelayDetailsUpdateService,
) {
    /**
     * Fetches and processes relay consensus descriptors of the last 3 days.
     * The 3 days of descriptors equals about 175 MB.
     * Can take 20 minutes depending on your machine.
     */
    @Scheduled(fixedRateString = "\${schedule.rate.recentRelayConsensuses}")
    fun recentRelayConsensuses() =
        torDescriptorService.collectAndProcessDescriptors(
            descriptorConfig.recentRelayConsensuses,
            DescriptorType.RECENT_RELAY_CONSENSUS
        )

    /**
     * Fetches and processes relay server descriptors of the last 3 days.
     * The 3 days of descriptors equals about 150 MB.
     * Can take 20 minutes depending on your machine.
     */
    @Scheduled(fixedRateString = "\${schedule.rate.recentRelayServers}")
    fun recentRelayServers() =
        torDescriptorService.collectAndProcessDescriptors(
            descriptorConfig.recentRelayServers,
            DescriptorType.RECENT_RELAY_SERVER
        )

    /**
     * Fetches and processes archive relay consensus descriptors.
     * The years 2007 - 2021 equal about 3 GB in size.
     * After the download finished, and you start with an empty DB this takes about 20 hours depending on your machine.
     */
    @Scheduled(fixedRateString = "\${schedule.rate.archiveRelayConsensuses}")
    fun archiveRelayConsensuses() =
        torDescriptorService.collectAndProcessDescriptors(
            descriptorConfig.archiveRelayConsensuses,
            DescriptorType.ARCHIVE_RELAY_CONSENSUS
        )

    /**
     * Fetches and processes archive relay server descriptors.
     * The years 2005 - 2021 equal about 30 GB in size.
     * After the download finished, and you start with an empty DB this takes about 10 hours depending on your machine.
     */
    @Scheduled(fixedRateString = "\${schedule.rate.archiveRelayServers}")
    fun archiveRelayServers() =
        torDescriptorService.collectAndProcessDescriptors(
            descriptorConfig.archiveRelayServers,
            DescriptorType.ARCHIVE_RELAY_SERVER
        )

    /**
     * Updates all relays which do not have a family set and optionally can overwrite existing family structures.
     * Can take a few minutes depending on how many months are updated.
     */
    @Scheduled(fixedRateString = "\${schedule.rate.updateRelayFamilies}")
    fun updateRelayFamilies() =
        relayDetailsUpdateService.updateAllFamilies(scheduleConfig.shouldOverwriteFamilies)

    /**
     * Updates all relays which do not have any Autonomous System set.
     * Can take a few minutes depending on how many months are updated.
     */
    @Scheduled(fixedRateString = "\${schedule.rate.updateRelayAutonomousSystems}")
    fun updateRelayAutonomousSystems() =
        relayDetailsUpdateService.updateAutonomousSystems()
}

