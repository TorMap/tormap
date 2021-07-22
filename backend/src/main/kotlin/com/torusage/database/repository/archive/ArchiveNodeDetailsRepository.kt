package com.torusage.database.repository.archive;

import com.torusage.database.entity.archive.ArchiveNodeDetails
import org.springframework.data.repository.CrudRepository

interface ArchiveNodeDetailsRepository : CrudRepository<ArchiveNodeDetails, Long> {
    fun getByMonthAndFingerprint(month: String, fingerprint: String): ArchiveNodeDetails?
}
