package org.tormap.database.entity

import org.springframework.boot.actuate.trace.http.HttpTrace
import java.time.Instant
import javax.persistence.*

@Suppress("unused")
@Entity
@Table(
    indexes = [
        Index(columnList = "timestamp"),
        Index(columnList = "method"),
        Index(columnList = "responseStatus"),
    ]
)
class UserTrace(trace: HttpTrace): AbstractBaseEntity<Long>() {
    var timestamp: Instant = trace.timestamp
    var uri: String = trace.request.uri.toString()

    @Enumerated
    var method: RequestMethod = RequestMethod.valueOf(trace.request.method.uppercase())
    var responseStatus: Int = trace.response.status
    var timeTaken: Long = trace.timeTaken
    var deviceClass: String? = null
    var operatingSystem: String? = null
    var agentMajorVersion: String? = null

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
