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

class DescriptorCoordinationServiceTest : StringSpec({
    "processLocalDescriptorFiles saves the final processed descriptor file" {
        val descriptorConfig = mockk<DescriptorConfig>(relaxed = true)
        val relayDetailsUpdateService = mockk<RelayDetailsUpdateService>(relaxed = true)
        val descriptorFileService = mockk<DescriptorFileService>(relaxed = true)
        val descriptorProcessingService = mockk<DescriptorProcessingService>()
        val relayDetailsRepository = mockk<RelayDetailsRepository>(relaxed = true)
        val relayLocationRepository = mockk<RelayLocationRepository>(relaxed = true)
        val cacheService = mockk<CacheService>(relaxed = true)
        val service = DescriptorCoordinationService(
            descriptorConfig,
            relayDetailsUpdateService,
            descriptorFileService,
            descriptorProcessingService,
            relayDetailsRepository,
            relayLocationRepository,
            cacheService,
        )
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

        DescriptorCoordinationService::class.java
            .getDeclaredMethod("processLocalDescriptorFiles", String::class.java, DescriptorType::class.java)
            .apply { isAccessible = true }
            .invoke(service, "/archive", DescriptorType.ARCHIVE_RELAY_CONSENSUS)

        verify(exactly = 1) {
            descriptorFileService.saveProcessedFileReference(firstFile, DescriptorType.ARCHIVE_RELAY_CONSENSUS)
        }
        verify(exactly = 1) {
            descriptorFileService.saveProcessedFileReference(finalFile, DescriptorType.ARCHIVE_RELAY_CONSENSUS)
        }
    }
})

private fun descriptorFrom(file: File): Descriptor {
    val descriptor = mockk<Descriptor>()
    every { descriptor.descriptorFile } returns file
    return descriptor
}
