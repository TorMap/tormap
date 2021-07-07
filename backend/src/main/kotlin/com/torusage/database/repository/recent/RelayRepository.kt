package com.torusage.database.repository.recent

import com.torusage.database.entity.recent.Relay
import org.springframework.data.repository.CrudRepository


/**
 * Repository to interact with DB table "RELAY"
 */
interface RelayRepository : CrudRepository<Relay, String> {
    fun findById(id: Long): Relay?
    fun findAllByLatitudeNotNullAndLongitudeNotNull(): Iterable<Relay>
}
