package com.torusage.service

import com.torusage.database.entity.DescriptorType
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


/**
 * This scheduler sets reoccurring events to collect data about Tor nodes
 */
@Component
class SchedulerService(
    val torDescriptorService: TorDescriptorService,
) {
    @Value("\${collector.path.relay.consensuses}")
    lateinit var collectorPathRelayConsensuses: String

    @Value("\${collector.path.relay.servers}")
    lateinit var collectorApiPathServerDescriptors: String

    /**
     * Fetches and processes relay consensus descriptors.
     * The years 2007 - 2021 equal roughly 3 GB in size.
     */
    @Scheduled(fixedRate = 86400000L)
    fun processRelayConsensusDescriptors() =
        torDescriptorService.collectAndProcessDescriptors(collectorPathRelayConsensuses, DescriptorType.RELAY_CONSENSUS)

    /**
     * Fetches and processes relay server descriptors.
     * The years 2005 - 2021 equal roughly 30 GB in size.
     */
    @Scheduled(fixedRate = 86400000L)
    fun collectRelayServerDescriptors() =
        torDescriptorService.collectAndProcessDescriptors(collectorApiPathServerDescriptors, DescriptorType.SERVER)
}

