package com.torusage.database.repository

import com.torusage.database.entity.Bridge
import org.springframework.data.repository.CrudRepository

interface BridgeRepository : CrudRepository<Bridge, Long>{
    fun findByNickname(nickname: String): Bridge?
    //fun findAllByOOrderByN(): Iterable<RelaySummary>
}
