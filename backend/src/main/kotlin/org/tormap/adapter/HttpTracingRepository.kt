package org.tormap.adapter

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.web.exchanges.HttpExchange
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Repository
import org.tormap.database.entity.UserTrace
import org.tormap.database.repository.UserTraceRepository
import org.tormap.service.IpLookupService
import ua_parser.Parser

object UserAgentParser : Parser()

@Repository
class HttpTraceRepository(
    private val userTraceRepository: UserTraceRepository,
    private val ipLookupService: IpLookupService,

    @Value("\${management.httpexchanges.recording.maxInMemory}")
    private val maxHttpTracesInMemory: Int
) : InMemoryHttpExchangeRepository() {
    private var unsavedTraces = 0

    init {
        super.setCapacity(maxHttpTracesInMemory)
    }

    override fun add(trace: HttpExchange) {
        if (unsavedTraces == maxHttpTracesInMemory) {
            saveTraces(super.findAll())
            unsavedTraces = 0
        }
        super.add(trace)
        unsavedTraces++
    }

    @Async
    fun saveTraces(httpTraces: List<HttpExchange>) {
        httpTraces.forEach {
            val userTrace = UserTrace(it)
            val ipAddress = it.request.remoteAddress ?: it.request.headers["X-FORWARDED-FOR"]?.firstOrNull()
            if (ipAddress != null) {
                val location = ipLookupService.lookupLocation(ipAddress)
                userTrace.countryCode = location?.countryCode
            }
            val userAgent = it.request.headers["user-agent"]?.firstOrNull()
            if (userAgent != null) {
                val userDeviceInfo = UserAgentParser.parse(userAgent)
                userTrace.deviceClass = userDeviceInfo.device.family
                userTrace.operatingSystem = "${userDeviceInfo.os.family} ${userDeviceInfo.os.major}"
                userTrace.agentMajorVersion = userDeviceInfo.userAgent.major
            }
            userTraceRepository.save(userTrace)
        }
        userTraceRepository.flush()
    }
}
