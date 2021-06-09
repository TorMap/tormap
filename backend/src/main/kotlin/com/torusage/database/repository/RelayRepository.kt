package com.torusage.database.repository

import com.torusage.database.entity.Relay
import org.springframework.data.repository.CrudRepository


/**
 * Repository to interact with DB table "RELAY"
 */
interface RelayRepository : CrudRepository<Relay, String> {
    fun findById(id: Long): Relay?
    fun findAllByLatitudeNotNullAndLongitudeNotNull(): Iterable<Relay>
}
