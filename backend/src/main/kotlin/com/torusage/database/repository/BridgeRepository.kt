package com.torusage.database.repository

import com.torusage.database.entity.Bridge
import org.springframework.data.repository.CrudRepository

/**
 * Repository to interact with DB table "BRIDGE"
 */
interface BridgeRepository : CrudRepository<Bridge, Long>{
    fun findByNickname(nickname: String): Bridge?
    //fun findAllByOOrderByN(): Iterable<RelaySummary>
}
