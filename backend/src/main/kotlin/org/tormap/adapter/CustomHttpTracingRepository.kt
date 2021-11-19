package org.tormap.adapter

import org.springframework.boot.actuate.trace.http.HttpTrace
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository
import org.springframework.stereotype.Repository
import org.tormap.database.entity.ApiTrace
import org.tormap.database.repository.ApiTraceRepository

@Repository
class CustomHttpTraceRepository(
    private val apiTraceRepository: ApiTraceRepository
) : InMemoryHttpTraceRepository() {
    private val saveTracesAtThreshold = 100
    private var unsavedTraces = 0

    init {
        super.setCapacity(saveTracesAtThreshold)
    }

    override fun add(trace: HttpTrace) {
        if (unsavedTraces == saveTracesAtThreshold) {
            apiTraceRepository.saveAll(super.findAll().map { ApiTrace(it) })
            unsavedTraces = 0
        }
        super.add(trace)
        unsavedTraces++
    }
}