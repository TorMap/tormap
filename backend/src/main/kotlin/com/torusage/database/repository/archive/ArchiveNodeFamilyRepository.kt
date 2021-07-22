package com.torusage.database.repository.archive;

import com.torusage.database.entity.archive.ArchiveNodeFamily
import org.springframework.data.repository.CrudRepository

interface ArchiveNodeFamilyRepository : CrudRepository<ArchiveNodeFamily, Long> {
    fun existsByMonthAndFingerprints(month: String, fingerprints: String): Boolean
}
