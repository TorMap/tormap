package org.tormap.service

import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.tormap.config.ApiConfig
import org.tormap.config.SchedulerConfig
import org.tormap.database.entity.DescriptorType


/**
 * This scheduler sets reoccurring events to collect and process data about Tor nodes
 * Functions marked with @Async will be run in parallel on separate threads if available.
 */
@Service
class SchedulerService(
    private val apiConfig: ApiConfig,
    private val schedulerConfig: SchedulerConfig,
    private val torDescriptorService: TorDescriptorService,
    private val nodeDetailsService: NodeDetailsService,
) {
    /**
     * Fetches and processes relay consensus descriptors.
     * The years 2007 - 2021 equal about 3 GB in size.
     * After the download finished and you start with an empty DB this takes about 20 hours depending on your machine.
     */
    @Async
    @Scheduled(fixedRateString = "\${scheduler.relayConsensusDescriptorsRate}")
    fun relayConsensusDescriptors() =
        torDescriptorService.collectAndProcessDescriptors(
            apiConfig.descriptorPathRelayConsensuses,
            DescriptorType.RELAY_CONSENSUS
        )

    /**
     * Fetches and processes relay server descriptors.
     * The years 2005 - 2021 equal about 30 GB in size.
     * After the download finished, and you start with an empty DB this takes about 10 hours depending on your machine.
     */
    @Async
    @Scheduled(fixedRateString = "\${scheduler.relayConsensusDescriptorsRate}")
    fun relayServerDescriptors() =
        torDescriptorService.collectAndProcessDescriptors(apiConfig.descriptorPathRelayServers, DescriptorType.SERVER)


    /**
     * Updates all months where no node families are set.
     * Can take a few minutes depending on how many months need to updated.
     */
    @Async
    @Scheduled(fixedRateString = "\${scheduler.updateNodeFamiliesRate}")
    fun updateNodeFamilies() =
        nodeDetailsService.updateNodeFamilies(null, schedulerConfig.updateNodeFamiliesOverwriteAll)

    /**
     * Updates all nodes which do not have any Autonomous System set.
     * Can take multiple hours depending on how many nodes need to be updated.
     */
    @Async
    @Scheduled(fixedRateString = "\${scheduler.updateNodeAutonomousSystemsRate}")
    fun updateNodeAutonomousSystems() =
        nodeDetailsService.updateAutonomousSystems()
}

