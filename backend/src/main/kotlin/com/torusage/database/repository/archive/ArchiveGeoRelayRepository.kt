@file:Suppress("FunctionName")

package com.torusage.database.repository.archive

import com.torusage.database.entity.archive.ArchiveGeoRelay
import com.torusage.database.entity.archive.ArchiveNodeId
import org.springframework.data.repository.CrudRepository
import java.util.*


/**
 * Repository to interact with DB
 */
interface ArchiveGeoRelayRepository : CrudRepository<ArchiveGeoRelay, ArchiveNodeId> {
    fun findAllByDay(day: Calendar): List<ArchiveGeoRelay>
}
