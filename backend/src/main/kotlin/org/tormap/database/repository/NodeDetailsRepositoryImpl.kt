package org.tormap.database.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.tormap.database.entity.NodeDetails

interface NodeDetailsRepositoryImpl : NodeDetailsRepository {
    @Query("SELECT DISTINCT month FROM NodeDetails ORDER BY month")
    fun findDistinctMonths(): Set<String>
}
