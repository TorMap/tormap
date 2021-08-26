package org.tormap.database.repository

import org.springframework.data.jpa.repository.Query
import org.tormap.database.entity.AutonomousSystem

interface AutonomousSystemRepositoryImpl : AutonomousSystemRepository {
    @Query("SELECT i FROM AutonomousSystem i WHERE i.ipRange.ipFrom <= :ipv4 AND i.ipRange.ipTo >= :ipv4")
    fun findUsingIPv4(ipv4: Long): AutonomousSystem?
}
