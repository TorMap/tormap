package com.torusage.database.repository

import com.torusage.database.entity.Bridge
import org.springframework.data.repository.CrudRepository

/**
 * Repository to interact with DB table "BRIDGE"
 */
interface BridgeRepository : CrudRepository<Bridge, String>
