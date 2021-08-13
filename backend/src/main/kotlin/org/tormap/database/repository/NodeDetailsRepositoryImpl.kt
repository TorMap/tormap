package org.tormap.database.repository

import org.springframework.data.jpa.repository.Query

interface NodeDetailsRepositoryImpl : NodeDetailsRepository {
    @Query("SELECT DISTINCT month FROM NodeDetails")
    fun findDistinctMonths(): Set<String>

    // TODO updateAutonomousSystems
//    @Transactional
//    @Modifying
//    @Query("")
//    fun updateAutonomousSystems(): Int
}
