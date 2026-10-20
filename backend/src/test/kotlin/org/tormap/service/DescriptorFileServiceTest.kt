package org.tormap.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.tormap.config.value.DescriptorConfig
import org.tormap.database.entity.DescriptorFileId
import org.tormap.database.entity.DescriptorType
import org.tormap.database.entity.ProcessedFile
import org.tormap.database.repository.ProcessedFileRepository
import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.time.Instant

@SpringBootTest
@ActiveProfiles("test")
class DescriptorFileServiceTest(
    private val descriptorConfig: DescriptorConfig,
    private val processedFileRepository: ProcessedFileRepository,
) : StringSpec({
    val downloadDirectory = Files.createTempDirectory("tormap-descriptors").toFile()
    val service = DescriptorFileService(
        descriptorConfig.copy(localDownloadDirectory = downloadDirectory.absolutePath),
        processedFileRepository,
    )
    val apiPath = "/recent/relay-descriptors/server-descriptors"

    beforeEach {
        processedFileRepository.deleteAll()
        downloadDirectory.listFiles()?.forEach { it.deleteRecursively() }
    }

    afterSpec {
        downloadDirectory.deleteRecursively()
    }

    fun writeDescriptorFile(name: String): File {
        val directory = File(downloadDirectory, apiPath).apply { mkdirs() }
        // The type annotation lets the reader pick the parser, the content itself does not parse
        return File(directory, name).apply { writeText("@type server-descriptor 1.0\nrouter broken\n") }
    }

    fun descriptorFileNames(type: DescriptorType = DescriptorType.RECENT_RELAY_SERVER) =
        service.getDescriptorDiskReader(apiPath, type).map { it.descriptorFile.name }

    "reads the descriptors of files that were not processed yet" {
        writeDescriptorFile("2024-03-05-00-00-00-server-descriptors")
        descriptorFileNames() shouldBe listOf("2024-03-05-00-00-00-server-descriptors")
    }

    "skips files that were processed unless they changed since" {
        val file = writeDescriptorFile("2024-03-05-00-00-00-server-descriptors")
        service.saveProcessedFileReference(file, DescriptorType.RECENT_RELAY_SERVER)
        descriptorFileNames().shouldBeEmpty()
        // Another descriptor type has its own processed files
        descriptorFileNames(DescriptorType.RECENT_RELAY_CONSENSUS) shouldBe listOf(file.name)

        file.setLastModified(file.lastModified() + 60_000)
        descriptorFileNames() shouldBe listOf(file.name)
    }

    "saving a processed file again updates its reference" {
        val file = writeDescriptorFile("2024-03-05-00-00-00-server-descriptors")
        service.saveProcessedFileReference(file, DescriptorType.ARCHIVE_RELAY_SERVER)
        val firstProcessedAt = processedFileRepository.findAll().single().processedAt
        Thread.sleep(5)
        service.saveProcessedFileReference(file, DescriptorType.ARCHIVE_RELAY_SERVER)
        val stored = processedFileRepository.findAll().single()
        stored.id shouldBe DescriptorFileId(DescriptorType.ARCHIVE_RELAY_SERVER, file.name)
        (stored.processedAt > firstProcessedAt) shouldBe true
    }

    "deletes only old references of recent files" {
        val old = Instant.now().minus(Duration.ofDays(DAYS_TO_KEEP_RECENT_FILES + 1)).toEpochMilli()
        val fresh = Instant.now().toEpochMilli()
        processedFileRepository.saveAll(
            listOf(
                ProcessedFile(DescriptorFileId(DescriptorType.RECENT_RELAY_SERVER, "old-recent"), old),
                ProcessedFile(DescriptorFileId(DescriptorType.RECENT_RELAY_CONSENSUS, "old-recent"), old),
                ProcessedFile(DescriptorFileId(DescriptorType.RECENT_RELAY_SERVER, "fresh-recent"), fresh),
                ProcessedFile(DescriptorFileId(DescriptorType.ARCHIVE_RELAY_SERVER, "old-archive"), old),
            )
        )

        service.deleteRecentFileReferences()

        processedFileRepository.findAll().map { it.id.type to it.id.filename }.toSet() shouldBe setOf(
            DescriptorType.RECENT_RELAY_SERVER to "fresh-recent",
            DescriptorType.ARCHIVE_RELAY_SERVER to "old-archive",
        )
    }

    "file ids are equal by type and name" {
        val id = DescriptorFileId(DescriptorType.RECENT_RELAY_SERVER, "a")
        id shouldBe DescriptorFileId(DescriptorType.RECENT_RELAY_SERVER, "a")
        id.hashCode() shouldBe DescriptorFileId(DescriptorType.RECENT_RELAY_SERVER, "a").hashCode()
        (id == DescriptorFileId(DescriptorType.ARCHIVE_RELAY_SERVER, "a")) shouldBe false
        (id == DescriptorFileId(DescriptorType.RECENT_RELAY_SERVER, "b")) shouldBe false
        id.equals("a") shouldBe false
    }

})
