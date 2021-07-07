package com.torusage.database.repository.recent

import com.torusage.database.entity.recent.BridgeSummary
import org.springframework.data.repository.CrudRepository

/**
 * Repository to interact with DB table "BRIDGE_SUMMARY"
 */
interface BridgeSummaryRepository : CrudRepository<BridgeSummary, String>
