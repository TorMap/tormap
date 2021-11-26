package org.tormap.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.tormap.database.entity.UserTrace

interface UserTraceRepository : JpaRepository<UserTrace, Long>
