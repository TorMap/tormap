package org.tormap.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.io.ClassPathResource
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.tormap.config.value.DescriptorConfig
import org.tormap.database.entity.DescriptorType

class SchedulingServiceTest : StringSpec({
    // With @Async, fixedDelay would no longer wait for the previous run and long runs would overlap
    "scheduled jobs are not async" {
        val scheduledMethods = SchedulingService::class.java.declaredMethods
            .filter { it.isAnnotationPresent(Scheduled::class.java) }
        scheduledMethods.shouldNotBeEmpty()

        SchedulingService::class.java.isAnnotationPresent(Async::class.java) shouldBe false
        scheduledMethods.filter { it.isAnnotationPresent(Async::class.java) }.shouldBeEmpty()
    }

    "the scheduler has a thread for each job" {
        val scheduledJobs = SchedulingService::class.java.declaredMethods
            .count { it.isAnnotationPresent(Scheduled::class.java) }
        val poolSize = YamlPropertiesFactoryBean().apply { setResources(ClassPathResource("application.yml")) }
            .getObject()!!
            .getProperty("spring.task.scheduling.pool.size")

        poolSize!!.toInt() shouldBe scheduledJobs
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
