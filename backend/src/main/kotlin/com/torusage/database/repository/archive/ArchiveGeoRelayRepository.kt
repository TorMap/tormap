@file:Suppress("FunctionName")

package com.torusage.database.repository.archive

import com.torusage.database.entity.archive.ArchiveGeoRelay
import com.torusage.database.entity.archive.ArchiveNodeId
import org.springframework.data.repository.CrudRepository
import java.time.LocalDate


/**
 * Repository to interact with DB
 */
interface ArchiveGeoRelayRepository : CrudRepository<ArchiveGeoRelay, ArchiveNodeId> {
    fun findAllByDay(day: LocalDate): List<ArchiveGeoRelay>
}
