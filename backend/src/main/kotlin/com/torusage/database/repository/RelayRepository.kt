package com.torusage.database.repository

import com.torusage.database.entity.Relay
import org.springframework.data.repository.CrudRepository

/**
 * Repository to interact with DB table "RELAY"
 */
interface RelayRepository : CrudRepository<Relay, Long>{
    fun findByNickname(nickname: String): Relay?
    //fun findAllByOOrderByN(): Iterable<RelaySummary>
}