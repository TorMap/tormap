package org.tormap.service

import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.tormap.config.value.DescriptorConfig
import org.tormap.database.entity.DescriptorType
import java.util.concurrent.TimeUnit


/**
 * This scheduler sets reoccurring events to collect and process data about Tor relays
 * With the @Async class annotation all methods run in parallel on separate threads if available.
 */
@Service
@Async
class SchedulingService(
    private val descriptorConfig: DescriptorConfig,
    private val descriptorCoordinationService: DescriptorCoordinationService,
    private val relayDetailsUpdateService: RelayDetailsUpdateService,
    private val descriptorFileService: DescriptorFileService,
) {
    /**
     * The most recent 3 days of descriptors equals about 175 MB.
     * Takes ~20 min to process.
     */
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    fun recentConsensousDescriptors() =
        descriptorCoordinationService.collectAndProcessDescriptors(
            descriptorConfig.recentRelayConsensuses,
            DescriptorType.RECENT_RELAY_CONSENSUS
        )

    /**
     * The most recent 3 days of descriptors equals about 150 MB.
     * Takes ~20 min to process after download.
     */
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    fun recentServerDescriptors() =
        descriptorCoordinationService.collectAndProcessDescriptors(
            descriptorConfig.recentRelayServers,
            DescriptorType.RECENT_RELAY_SERVER
        )

    /**
     * The years 2007 - 2021 equal about 3 GB in size.
     * Takes ~12 hours to process after download.
     */
    @Scheduled(fixedDelay = 12, timeUnit = TimeUnit.HOURS)
    fun archiveRelayConsensuses() =
        descriptorCoordinationService.collectAndProcessDescriptors(
            descriptorConfig.archiveRelayConsensuses,
            DescriptorType.ARCHIVE_RELAY_CONSENSUS
        )

    /**
     * The years 2005 - 2021 equal about 30 GB in size.
     * Takes ~10 hours to process after download.
     */
    @Scheduled(fixedDelay = 12, timeUnit = TimeUnit.HOURS)
    fun archiveRelayServers() =
        descriptorCoordinationService.collectAndProcessDescriptors(
            descriptorConfig.archiveRelayServers,
            DescriptorType.ARCHIVE_RELAY_SERVER
        )

    @Scheduled(fixedDelay = 1, initialDelay = 1, timeUnit = TimeUnit.DAYS)
    fun updateRelayFamilies() =
        relayDetailsUpdateService.computeAllMissingFamilies()

    @Scheduled(fixedDelay = 1, initialDelay = 1, timeUnit = TimeUnit.DAYS)
    fun updateRelayAutonomousSystems() =
        relayDetailsUpdateService.lookupAllMissingAutonomousSystems()

    @Scheduled(fixedDelayString = "P1D", initialDelay = DAYS_TO_KEEP_RECENT_FILES, timeUnit = TimeUnit.DAYS)
    fun deleteRecentFileReference() =
        descriptorFileService.deleteRecentFileReferences()
}

