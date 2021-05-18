package com.torusage.database

import com.torusage.database.entities.RelaySummary
import org.springframework.data.repository.CrudRepository

interface RelaySummaryRepositories : CrudRepository<RelaySummary, Long>{
    fun findByN(n: String): RelaySummary?
    //fun findAllByOOrderByN(): Iterable<RelaySummary>
}
