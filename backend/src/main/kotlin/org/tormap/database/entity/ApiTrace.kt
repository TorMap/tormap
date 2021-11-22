package org.tormap.database.entity

import org.springframework.boot.actuate.trace.http.HttpTrace
import java.time.Instant
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.Id

@Suppress("unused")
@Entity
class ApiTrace(trace: HttpTrace) {
    @Id
    var timestamp: Instant = trace.timestamp
    var uri: String = trace.request.uri.toString()

    @Enumerated
    var method: RequestMethod = RequestMethod.valueOf(trace.request.method.uppercase())
    var responseStatus: Int = trace.response.status
    var userAgent: String? = trace.request.headers["user-agent"]?.firstOrNull()
}

enum class RequestMethod(method: String) {
    GET("GET"),
    HEAD("HEAD"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    CONNECT("CONNECT"),
    OPTIONS("OPTIONS"),
    TRACE("TRACE"),
    PATCH("PATCH"),
}
