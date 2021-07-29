package com.torusage.service

import com.torusage.config.ApiConfig
import com.torusage.database.entity.DescriptorType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


/**
 * This scheduler sets reoccurring events to collect data about Tor nodes
 */
@Component
class SchedulerService(
    val apiConfig: ApiConfig,
    val torDescriptorService: TorDescriptorService,
) {

    /**
     * Updates the foreign ids for all available data
     * Takes about 1 minute depending on your machine.
     */
//    @Scheduled(fixedRate = 86400000L)
    fun updateAllGeoRelayForeignIds() =
        torDescriptorService.updateAllGeoRelayForeignIds()

    /**
     * Fetches and processes relay consensus descriptors.
     * The years 2007 - 2021 equal about 3 GB in size.
     * After the download finished and you start with an empty DB this takes about 20 hours depending on your machine.
     */
    @Scheduled(fixedRateString = "\${scheduler.relayConsensusDescriptorsRate}")
    fun handleRelayConsensusDescriptors() =
        torDescriptorService.collectAndProcessDescriptors(
            apiConfig.descriptorPathRelayConsensuses,
            DescriptorType.RELAY_CONSENSUS
        )

    /**
     * Fetches and processes relay server descriptors.
     * The years 2005 - 2021 equal about 30 GB in size.
     * After the download finished and you start with an empty DB this takes about 10 hours depending on your machine.
     */
    @Scheduled(fixedRateString = "\${scheduler.relayConsensusDescriptorsRate}")
    fun handleRelayServerDescriptors() =
        torDescriptorService.collectAndProcessDescriptors(apiConfig.descriptorPathRelayServers, DescriptorType.SERVER)
}

