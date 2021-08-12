package org.tormap.service

import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.tormap.config.ApiConfig
import org.tormap.database.entity.DescriptorType
import org.tormap.database.repository.NodeDetailsRepositoryImpl


/**
 * This scheduler sets reoccurring events to collect and process data about Tor nodes
 * Functions marked with @Async will be run in parallel on separate threads if available.
 */
@Component
class SchedulerService(
    val apiConfig: ApiConfig,
    val torDescriptorService: TorDescriptorService,
    val nodeDetailsService: NodeDetailsService,
    val nodeDetailsRepositoryImpl: NodeDetailsRepositoryImpl,
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
     * After the download finished and you start with an empty DB this takes about 10 hours depending on your machine.
     */
    @Async
    @Scheduled(fixedRateString = "\${scheduler.relayConsensusDescriptorsRate}")
    fun relayServerDescriptors() =
        torDescriptorService.collectAndProcessDescriptors(apiConfig.descriptorPathRelayServers, DescriptorType.SERVER)

    /**
     * Updates the node families in DB
     * Can take up to 30 min depending on your machine.
     */
    @Async
    @Scheduled(fixedRateString = "\${scheduler.updateAllNodeFamiliesRate}")
    fun updateAllNodeFamilies() =
        nodeDetailsService.updateNodeFamilies(nodeDetailsRepositoryImpl.findDistinctMonths())
}

