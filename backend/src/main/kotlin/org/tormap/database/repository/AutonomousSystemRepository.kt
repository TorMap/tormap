package org.tormap.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.CrudRepository
import org.tormap.database.entity.AutonomousSystem
import org.tormap.database.entity.IpRangeId

interface AutonomousSystemRepository : JpaRepository<AutonomousSystem, IpRangeId>
