package org.tormap.service

import io.kotest.core.spec.style.StringSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.tormap.config.value.DescriptorConfig
import org.tormap.database.entity.DescriptorType
import org.tormap.database.repository.RelayDetailsRepository
import org.tormap.database.repository.RelayLocationRepository
import org.torproject.descriptor.Descriptor
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CompletableFuture

class DescriptorCoordinationServiceTest : StringSpec({
    lateinit var relayDetailsUpdateService: RelayDetailsUpdateService
    lateinit var descriptorFileService: DescriptorFileService
    lateinit var descriptorProcessingService: DescriptorProcessingService
    lateinit var cacheService: CacheService
    lateinit var service: DescriptorCoordinationService
    lateinit var relayLocationRepository: RelayLocationRepository
    val downloadDirectory = Files.createTempDirectory("tormap-collect").toFile()
    afterSpec { downloadDirectory.deleteRecursively() }

    beforeEach {
        relayDetailsUpdateService = mockk(relaxed = true)
        descriptorFileService = mockk(relaxed = true)
        descriptorProcessingService = mockk()
        cacheService = mockk(relaxed = true)
        relayLocationRepository = mockk(relaxed = true)
        val descriptorConfig = mockk<DescriptorConfig>(relaxed = true)
        // Nothing listens on port 1, so collecting fails right away without network access
        every { descriptorConfig.apiBaseURL } returns "http://127.0.0.1:1"
        every { descriptorConfig.localDownloadDirectory } returns downloadDirectory.absolutePath
        service = DescriptorCoordinationService(
            descriptorConfig,
            relayDetailsUpdateService,
            descriptorFileService,
            descriptorProcessingService,
            mockk<RelayDetailsRepository>(relaxed = true),
            relayLocationRepository,
            cacheService,
        )
    }

    fun processLocalDescriptorFiles(apiPath: String, descriptorType: DescriptorType) {
        DescriptorCoordinationService::class.java
            .getDeclaredMethod("processLocalDescriptorFiles", String::class.java, DescriptorType::class.java)
            .apply { isAccessible = true }
            .invoke(service, apiPath, descriptorType)
    }

    "processLocalDescriptorFiles saves the final processed descriptor file" {
        val firstFile = File("first-descriptors.tar.xz")
        val finalFile = File("final-descriptors.tar.xz")
        val descriptors = mutableListOf(
            descriptorFrom(firstFile),
            descriptorFrom(firstFile),
            descriptorFrom(finalFile),
        )

        every {
            descriptorFileService.getDescriptorDiskReader("/archive", DescriptorType.ARCHIVE_RELAY_CONSENSUS)
        } returns descriptors
        every { descriptorProcessingService.processDescriptor(any()) } returns ProcessedDescriptorInfo("2024-01")

        processLocalDescriptorFiles("/archive", DescriptorType.ARCHIVE_RELAY_CONSENSUS)

        verify(exactly = 1) {
            descriptorFileService.saveProcessedFileReference(firstFile, DescriptorType.ARCHIVE_RELAY_CONSENSUS)
        }
        verify(exactly = 1) {
            descriptorFileService.saveProcessedFileReference(finalFile, DescriptorType.ARCHIVE_RELAY_CONSENSUS)
        }
        verify(exactly = 2) { cacheService.cacheRelayLocationDistinctDays() }
        verify(exactly = 2) { cacheService.cacheRelayDailyCounts() }
        verify(exactly = 2) { cacheService.evictRelayLocationsPerDay(setOf("2024-01")) }
        verify(exactly = 0) { relayDetailsUpdateService.computeFamilies(any()) }
    }

    "files with errors are not saved as processed" {
        val file = File("broken-descriptors.tar.xz")
        every {
            descriptorFileService.getDescriptorDiskReader("/archive", DescriptorType.ARCHIVE_RELAY_CONSENSUS)
        } returns mutableListOf(descriptorFrom(file), descriptorFrom(file))
        every { descriptorProcessingService.processDescriptor(any()) } returnsMany listOf(
            ProcessedDescriptorInfo("2024-01"),
            ProcessedDescriptorInfo(error = "unparsable"),
        )

        processLocalDescriptorFiles("/archive", DescriptorType.ARCHIVE_RELAY_CONSENSUS)

        verify(exactly = 0) { descriptorFileService.saveProcessedFileReference(any(), any()) }
    }

    "server descriptors only update the cached relay locations once the families are computed" {
        val file = File("server-descriptors.tar.xz")
        val families = CompletableFuture<Void>()
        every {
            descriptorFileService.getDescriptorDiskReader("/archive", DescriptorType.ARCHIVE_RELAY_SERVER)
        } returns mutableListOf(descriptorFrom(file))
        every { descriptorProcessingService.processDescriptor(any()) } returns ProcessedDescriptorInfo("2024-02")
        every { relayDetailsUpdateService.computeFamilies(setOf("2024-02")) } returns families

        processLocalDescriptorFiles("/archive", DescriptorType.ARCHIVE_RELAY_SERVER)

        verify { relayDetailsUpdateService.lookupMissingAutonomousSystems(setOf("2024-02")) }
        verify(exactly = 0) { cacheService.evictRelayLocationsPerDay(any()) }

        families.complete(null)

        verify(exactly = 1) { cacheService.evictRelayLocationsPerDay(setOf("2024-02")) }
        // Server descriptors do not change the days or daily counts
        verify(exactly = 0) { cacheService.cacheRelayLocationDistinctDays() }
        verify(exactly = 0) { cacheService.cacheRelayDailyCounts() }
    }

    "recent server descriptors refresh the cached relay locations after the families, also if they failed" {
        val file = File("recent-server-descriptors")
        val families = CompletableFuture<Void>()
        every {
            descriptorFileService.getDescriptorDiskReader("/recent", DescriptorType.RECENT_RELAY_SERVER)
        } returns mutableListOf(descriptorFrom(file))
        every { descriptorProcessingService.processDescriptor(any()) } returns ProcessedDescriptorInfo("2024-03")
        every { relayDetailsUpdateService.computeFamilies(setOf("2024-03")) } returns families

        processLocalDescriptorFiles("/recent", DescriptorType.RECENT_RELAY_SERVER)

        // Once for the finished file
        verify(exactly = 1) { cacheService.cacheRelayLocationsPerDay(setOf("2024-03")) }

        families.completeExceptionally(IllegalStateException("failed"))

        verify(exactly = 2) { cacheService.cacheRelayLocationsPerDay(setOf("2024-03")) }
    }

    "recent consensuses cache the relay locations without computing families" {
        val file = File("recent-consensuses")
        every {
            descriptorFileService.getDescriptorDiskReader("/recent", DescriptorType.RECENT_RELAY_CONSENSUS)
        } returns mutableListOf(descriptorFrom(file))
        every { descriptorProcessingService.processDescriptor(any()) } returns ProcessedDescriptorInfo("2024-04")

        processLocalDescriptorFiles("/recent", DescriptorType.RECENT_RELAY_CONSENSUS)

        verify(exactly = 2) { cacheService.cacheRelayLocationsPerDay(setOf("2024-04")) }
        verify(exactly = 2) { cacheService.cacheRelayDailyCounts() }
        verify(exactly = 0) { relayDetailsUpdateService.computeFamilies(any()) }
    }

    "collectAndProcessDescriptors processes local files also if the remote server is not reachable" {
        val file = File("recent-consensuses")
        every {
            descriptorFileService.getDescriptorDiskReader("/recent", DescriptorType.RECENT_RELAY_CONSENSUS)
        } returns mutableListOf(descriptorFrom(file))
        every { descriptorProcessingService.processDescriptor(any()) } returns ProcessedDescriptorInfo("2024-04")

        service.collectAndProcessDescriptors("/recent", DescriptorType.RECENT_RELAY_CONSENSUS)

        verify { relayLocationRepository.flush() }
        verify { descriptorFileService.saveProcessedFileReference(file, DescriptorType.RECENT_RELAY_CONSENSUS) }
    }

    "collectAndProcessDescriptors does not throw if processing fails" {
        every { descriptorFileService.getDescriptorDiskReader(any(), any()) } throws IllegalStateException("disk")
        service.collectAndProcessDescriptors("/recent", DescriptorType.RECENT_RELAY_CONSENSUS)
        verify(exactly = 0) { cacheService.cacheRelayDailyCounts() }
    }

    "a failing flush does not stop the processing" {
        val file = File("archive-consensuses")
        every {
            descriptorFileService.getDescriptorDiskReader("/archive", DescriptorType.ARCHIVE_RELAY_CONSENSUS)
        } returns mutableListOf(descriptorFrom(file))
        every { descriptorProcessingService.processDescriptor(any()) } returns ProcessedDescriptorInfo("2024-05")
        every { relayLocationRepository.flush() } throws IllegalStateException("database gone")

        processLocalDescriptorFiles("/archive", DescriptorType.ARCHIVE_RELAY_CONSENSUS)

        verify { descriptorFileService.saveProcessedFileReference(file, DescriptorType.ARCHIVE_RELAY_CONSENSUS) }
    }
})

private fun descriptorFrom(file: File): Descriptor {
    val descriptor = mockk<Descriptor>()
    every { descriptor.descriptorFile } returns file
    return descriptor
}
