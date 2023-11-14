package org.tormap.service

import org.springframework.stereotype.Service
import org.tormap.config.value.DescriptorConfig
import org.tormap.database.entity.DescriptorFileId
import org.tormap.database.entity.DescriptorType
import org.tormap.database.entity.ProcessedFile
import org.tormap.database.repository.ProcessedFileRepository
import org.torproject.descriptor.Descriptor
import org.torproject.descriptor.impl.DescriptorReaderImpl
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

const val DAYS_TO_KEEP_RECENT_FILES = 4L

@Service
class DescriptorFileService(
    private val descriptorConfig: DescriptorConfig,
    private val processedFileRepository: ProcessedFileRepository,
) {
    /**
     * Read descriptors which were previously saved to disk at [apiPath].
     * A reader can consume quite some memory. Try not to create multiple readers in a short time.
     */
    fun getDescriptorDiskReader(apiPath: String, descriptorType: DescriptorType): MutableIterable<Descriptor> {
        val descriptorReader = DescriptorReaderImpl()
        val parentDirectory = File(descriptorConfig.localDownloadDirectory + apiPath)
        val excludedFiles = processedFileRepository.findAllById_TypeEquals(descriptorType)
        descriptorReader.excludedFiles = excludedFiles.associate {
            Pair(
                parentDirectory.absolutePath + File.separator + it.id.filename,
                it.lastModified,
            )
        }.toSortedMap()

        return descriptorReader.readDescriptors(parentDirectory)
    }

    /**
     * Saves a reference of the finished [descriptorFile] to the DB.
     * In the future it will be excluded from processing if no newer version is discovered on the remote server.
     */
    fun saveProcessedFileReference(descriptorFile: File, descriptorType: DescriptorType) {
        val descriptorsDescriptorFileId = DescriptorFileId(descriptorType, descriptorFile.name)
        val descriptorsFile = processedFileRepository.findById(descriptorsDescriptorFileId).orElseGet {
            ProcessedFile(
                descriptorsDescriptorFileId,
                descriptorFile.lastModified(),
            )
        }
        descriptorsFile.processedAt = LocalDateTime.now()
        processedFileRepository.saveAndFlush(descriptorsFile)
    }

    fun deleteRecentFileReferences() {
        processedFileRepository.deleteById_TypeInAndLastModifiedBefore(
            listOf(DescriptorType.RECENT_RELAY_CONSENSUS, DescriptorType.RECENT_RELAY_SERVER),
            Instant.now().minus(DAYS_TO_KEEP_RECENT_FILES, ChronoUnit.DAYS).toEpochMilli()
        )
    }
}
