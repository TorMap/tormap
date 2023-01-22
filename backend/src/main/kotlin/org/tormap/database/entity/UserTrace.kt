package org.tormap.database.entity

import org.springframework.boot.actuate.web.exchanges.HttpExchange
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("user_trace")
@Suppress("unused", "MemberVisibilityCanBePrivate")
class UserTrace @PersistenceCreator internal constructor(
    @Id private var id: Long? = null,
    var timestamp: Instant,
    var uri: String,
    var method: RequestMethod,
    var responseStatus: Int,
    var timeTaken: Long,
    var deviceClass: String? = null,
    var operatingSystem: String? = null,
    var agentMajorVersion: String? = null,
    var countryCode: String? = null
) : Persistable<Long> {
    constructor(trace: HttpExchange) : this(
        timestamp = trace.timestamp,
        uri = trace.request.uri.toString(),
        method = RequestMethod.valueOf(trace.request.method.uppercase()),
        responseStatus = trace.response.status,
        timeTaken = trace.timeTaken.toSeconds()
    )

    override fun getId(): Long? = id

    override fun isNew(): Boolean = id != null
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
