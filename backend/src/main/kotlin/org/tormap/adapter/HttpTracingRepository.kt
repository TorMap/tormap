package org.tormap.adapter

import nl.basjes.parse.useragent.UserAgentAnalyzer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.trace.http.HttpTrace
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Repository
import org.tormap.database.entity.UserTrace
import org.tormap.database.repository.UserTraceRepository
import org.tormap.service.IpLookupService

const val DEVICE_CLASS = "DeviceClass"
const val OPERATING_SYSTEM_NAME = "OperatingSystemName"
const val AGENT_NAME_VERSION_MAJOR = "AgentNameVersionMajor"

@Repository
class HttpTraceRepository(
    private val userTraceRepository: UserTraceRepository,
    private val ipLookupService: IpLookupService,

    @Value("\${management.trace.http.maxInMemory}")
    private val maxHttpTracesInMemory: Int,
) : InMemoryHttpTraceRepository() {
    private var unsavedTraces = 0
    private val userAgentAnalyzer =
        UserAgentAnalyzer
            .newBuilder()
            .hideMatcherLoadStats()
            .withCache(1000)
            .withFields(DEVICE_CLASS, OPERATING_SYSTEM_NAME, AGENT_NAME_VERSION_MAJOR)
            .build();

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
            val userTrace = UserTrace(it)
            val ipAddress = it.request.remoteAddress ?: it.request.headers["X-FORWARDED-FOR"]?.firstOrNull()
            if (ipAddress != null) {
                val location = ipLookupService.lookupLocation(ipAddress)
                userTrace.countryCode = location?.countryCode
            }
            val userAgent = it.request.headers["user-agent"]?.firstOrNull()
            if (userAgent != null) {
                val userDeviceInfo = userAgentAnalyzer.parse(userAgent)
                userTrace.deviceClass = userDeviceInfo.getValue(DEVICE_CLASS)
                userTrace.operatingSystem = userDeviceInfo.getValue(OPERATING_SYSTEM_NAME)
                userTrace.agentMajorVersion = userDeviceInfo.getValue(AGENT_NAME_VERSION_MAJOR)
            }
            userTraceRepository.save(userTrace)
        }
        userTraceRepository.flush()
    }
}
