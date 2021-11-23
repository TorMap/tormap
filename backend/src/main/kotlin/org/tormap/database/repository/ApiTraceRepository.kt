package org.tormap.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.tormap.database.entity.ApiTrace
import java.time.Instant

interface ApiTraceRepository : JpaRepository<ApiTrace, Instant>
