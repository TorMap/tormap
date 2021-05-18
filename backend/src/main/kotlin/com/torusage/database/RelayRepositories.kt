package com.torusage.database

import com.torusage.database.entities.Relay
import org.springframework.data.repository.CrudRepository

interface RelayRepositories : CrudRepository<Relay, Long>{
    fun findByNickname(nickname: String): Relay?
    //fun findAllByOOrderByN(): Iterable<RelaySummary>
}
