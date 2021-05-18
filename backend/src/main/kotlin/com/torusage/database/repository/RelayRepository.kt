package com.torusage.database.repository

import com.torusage.database.entity.Relay
import org.springframework.data.repository.CrudRepository

interface RelayRepository : CrudRepository<Relay, Long>{
    fun findByNickname(nickname: String): Relay?
    //fun findAllByOOrderByN(): Iterable<RelaySummary>
}
