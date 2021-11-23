package org.tormap.database.entity

import org.springframework.boot.actuate.trace.http.HttpTrace
import java.time.Instant
import javax.persistence.*

@Suppress("unused")
@Entity
@Table(
    indexes = [
        Index(columnList = "timestamp", name = "timestamp_index"),
        Index(columnList = "method", name = "method_index"),
        Index(columnList = "responseStatus", name = "responseStatus_index"),
    ]
)
class ApiTrace(trace: HttpTrace) {
    @Id
    @GeneratedValue
    val id: Long? = null
    var timestamp: Instant = trace.timestamp
    var uri: String = trace.request.uri.toString()

    @Enumerated
    var method: RequestMethod = RequestMethod.valueOf(trace.request.method.uppercase())
    var responseStatus: Int = trace.response.status
    var userAgent: String? = trace.request.headers["user-agent"]?.firstOrNull()
    var timeTaken: Long = trace.timeTaken

    @Column(length = 2, columnDefinition = "char(2)")
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
