package org.tormap.database.repository

import org.springframework.data.repository.CrudRepository
import org.tormap.database.entity.ApiTrace
import java.time.Instant

interface ApiTraceRepository : CrudRepository<ApiTrace, Instant>
