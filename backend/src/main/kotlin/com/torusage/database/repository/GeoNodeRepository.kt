package com.torusage.database.repository

import com.torusage.database.entity.GeoNode
import org.springframework.data.repository.CrudRepository


/**
 * Repository to interact with DB table "RELAY"
 */
interface GeoNodeRepository : CrudRepository<GeoNode, String> {
}
