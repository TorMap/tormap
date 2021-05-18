package com.torusage.database.repository

import com.torusage.database.entity.RelaySummary
import org.springframework.data.repository.CrudRepository

interface RelaySummaryRepository : CrudRepository<RelaySummary, Long>{
    fun findByN(n: String): RelaySummary?
    //fun findAllByOOrderByN(): Iterable<RelaySummary>
}
