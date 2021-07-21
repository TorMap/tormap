package com.torusage.database.repository.archive;

import com.torusage.database.entity.archive.ArchiveNodeDetails
import org.springframework.data.repository.CrudRepository
import java.time.LocalDate

interface ArchiveNodeDetailsRepository : CrudRepository<ArchiveNodeDetails, Long> {
    fun existsByDayAndFingerprint(day: LocalDate, fingerprint: String): Boolean
}
