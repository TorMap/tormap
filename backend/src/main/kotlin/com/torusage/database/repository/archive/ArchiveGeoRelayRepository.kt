@file:Suppress("FunctionName")

package com.torusage.database.repository.archive

import com.torusage.database.entity.archive.ArchiveGeoRelay
import com.torusage.database.entity.archive.ArchiveNodeId
import org.springframework.data.repository.CrudRepository


/**
 * Repository to interact with DB
 */
interface ArchiveGeoRelayRepository : CrudRepository<ArchiveGeoRelay, ArchiveNodeId> {
    fun findAllById_SeenInMonth(seenInMonth: String): List<ArchiveGeoRelay>
}
