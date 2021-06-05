package com.torusage.database.repository

import com.torusage.database.entity.BridgeSummary
import com.torusage.database.entity.RelaySummary
import org.springframework.data.repository.CrudRepository

/**
 * Repository to interact with DB table "BRIDGE_SUMMARY"
 */
interface BridgeSummaryRepository : CrudRepository<BridgeSummary, Long>
