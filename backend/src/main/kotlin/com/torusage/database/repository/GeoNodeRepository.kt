package com.torusage.database.repository

import com.torusage.database.entity.GeoNode
import com.torusage.database.entity.GeoNodeId
import org.springframework.data.repository.CrudRepository


/**
 * Repository to interact with DB
 */
interface GeoNodeRepository : CrudRepository<GeoNode, GeoNodeId>
