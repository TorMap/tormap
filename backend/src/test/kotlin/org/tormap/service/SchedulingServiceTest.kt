package org.tormap.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.scheduling.annotation.Scheduled
import org.tormap.config.value.DescriptorConfig
import org.tormap.database.entity.DescriptorType
import java.util.concurrent.Future

class SchedulingServiceTest : StringSpec({
    // SchedulingService is annotated with @Async, which rejects other return types at runtime
    "all scheduled methods return void or Future" {
        val scheduledMethods = SchedulingService::class.java.declaredMethods
            .filter { it.isAnnotationPresent(Scheduled::class.java) }
        scheduledMethods.shouldNotBeEmpty()

        scheduledMethods
            .filterNot { it.returnType == Void.TYPE || Future::class.java.isAssignableFrom(it.returnType) }
            .map { "${it.name}: ${it.returnType}" }
            .shouldBeEmpty()
    }

    "each scheduled method starts its job" {
        val descriptorConfig = mockk<DescriptorConfig>()
        every { descriptorConfig.recentRelayConsensuses } returns "/recent/consensuses"
        every { descriptorConfig.recentRelayServers } returns "/recent/servers"
        every { descriptorConfig.archiveRelayConsensuses } returns "/archive/consensuses"
        every { descriptorConfig.archiveRelayServers } returns "/archive/servers"
        val coordination = mockk<DescriptorCoordinationService>(relaxed = true)
        val relayDetails = mockk<RelayDetailsUpdateService>(relaxed = true)
        val files = mockk<DescriptorFileService>(relaxed = true)
        val countryUsage = mockk<CountryUsageService>(relaxed = true)
        val service = SchedulingService(descriptorConfig, coordination, relayDetails, files, countryUsage)

        service.recentConsensousDescriptors()
        service.recentServerDescriptors()
        service.archiveRelayConsensuses()
        service.archiveRelayServers()
        service.refreshCountryUsage()
        service.updateRelayFamilies()
        service.updateRelayAutonomousSystems()
        service.deleteRecentFileReference()

        verify { coordination.collectAndProcessDescriptors("/recent/consensuses", DescriptorType.RECENT_RELAY_CONSENSUS) }
        verify { coordination.collectAndProcessDescriptors("/recent/servers", DescriptorType.RECENT_RELAY_SERVER) }
        verify { coordination.collectAndProcessDescriptors("/archive/consensuses", DescriptorType.ARCHIVE_RELAY_CONSENSUS) }
        verify { coordination.collectAndProcessDescriptors("/archive/servers", DescriptorType.ARCHIVE_RELAY_SERVER) }
        verify { countryUsage.refresh() }
        verify { relayDetails.computeAllMissingFamiliesAndEvictCache() }
        verify { relayDetails.lookupAllMissingAutonomousSystems() }
        verify { files.deleteRecentFileReferences() }
    }
})
