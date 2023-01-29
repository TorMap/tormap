package org.tormap.adapter

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.web.exchanges.HttpExchange
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository
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
    @Value("\${management.httpexchanges.recording.maxInMemory}") private val maxHttpTracesInMemory: Int
) : InMemoryHttpExchangeRepository() {

    init {
        super.setCapacity(maxHttpTracesInMemory)
    }

    override fun add(trace: HttpExchange) {
        super.add(trace)

        val userTrace = UserTrace(trace).apply {
            val ipAddress = trace.request.remoteAddress ?: trace.request.headers["X-FORWARDED-FOR"]?.firstOrNull()
            if (ipAddress != null) {
                countryCode = ipLookupService.lookupLocation(ipAddress)?.countryCode
            }
            val userAgent = trace.request.headers["user-agent"]?.firstOrNull()
            if (userAgent != null) {
                val userDeviceInfo = UserAgentParser.parse(userAgent)
                deviceClass = userDeviceInfo.device.family
                operatingSystem = "${userDeviceInfo.os.family} ${userDeviceInfo.os.major}"
                agentMajorVersion = userDeviceInfo.userAgent.major
            }
        }

        userTraceRepository.save(userTrace)
    }
}
