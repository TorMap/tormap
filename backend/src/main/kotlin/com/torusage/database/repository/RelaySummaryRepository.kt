package com.torusage.database.repository

import com.torusage.database.entity.RelaySummary
import org.springframework.data.repository.CrudRepository

/**
 * Repository to interact with DB table "relay_summary"
 */
interface RelaySummaryRepository : CrudRepository<RelaySummary, String>
