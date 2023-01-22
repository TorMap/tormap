package org.tormap.database.entity

import org.springframework.boot.actuate.web.exchanges.HttpExchange
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Suppress("unused")
@Table("user_trace")
class UserTrace(trace: HttpExchange) : AbstractBaseEntity<Long>() {
    var timestamp: Instant = trace.timestamp
    var uri: String = trace.request.uri.toString()
    var method: RequestMethod = RequestMethod.valueOf(trace.request.method.uppercase())
    var responseStatus: Int = trace.response.status
    var timeTaken: Long = trace.timeTaken.toSeconds()
    var deviceClass: String? = null
    var operatingSystem: String? = null
    var agentMajorVersion: String? = null
    var countryCode: String? = null
}

enum class RequestMethod {
    GET,
    HEAD,
    POST,
    PUT,
    DELETE,
    CONNECT,
    OPTIONS,
    TRACE,
    PATCH,
}
