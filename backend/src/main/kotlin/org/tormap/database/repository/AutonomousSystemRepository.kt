package org.tormap.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.tormap.database.entity.AutonomousSystem
import org.tormap.database.entity.IpRangeId

interface AutonomousSystemRepository : JpaRepository<AutonomousSystem, IpRangeId>
