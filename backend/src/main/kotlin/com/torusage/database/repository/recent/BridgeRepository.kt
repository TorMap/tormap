package com.torusage.database.repository.recent

import com.torusage.database.entity.recent.Bridge
import org.springframework.data.repository.CrudRepository

/**
 * Repository to interact with DB table "BRIDGE"
 */
interface BridgeRepository : CrudRepository<Bridge, String>
