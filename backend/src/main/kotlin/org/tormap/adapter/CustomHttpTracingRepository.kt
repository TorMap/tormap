package org.tormap.adapter

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.trace.http.HttpTrace
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Repository
import org.tormap.database.entity.ApiTrace
import org.tormap.database.repository.ApiTraceRepository
import org.tormap.service.IpLookupService

@Repository
class CustomHttpTraceRepository(
    private val apiTraceRepository: ApiTraceRepository,
    private val ipLookupService: IpLookupService,

    @Value("\${management.trace.http.maxInMemory}")
    private val maxHttpTracesInMemory: Int,
) : InMemoryHttpTraceRepository() {
    private var unsavedTraces = 0

    init {
        super.setCapacity(maxHttpTracesInMemory)
    }

    override fun add(trace: HttpTrace) {
        if (unsavedTraces == maxHttpTracesInMemory) {
            saveTraces(super.findAll())
            unsavedTraces = 0
        }
        super.add(trace)
        unsavedTraces++
    }

    @Async
    fun saveTraces(httpTraces: List<HttpTrace>){
        httpTraces.forEach {
            val apiTrace = ApiTrace(it)
            val ipAddress = it.request.remoteAddress ?: it.request.headers["X-FORWARDED-FOR"]?.firstOrNull()
            if (ipAddress != null) {
                val location = ipLookupService.getLocationForIpAddress(ipAddress)
                apiTrace.countryCode = location?.countryCode
            }
            apiTraceRepository.save(apiTrace)
        }
        apiTraceRepository.flush()
    }
}
