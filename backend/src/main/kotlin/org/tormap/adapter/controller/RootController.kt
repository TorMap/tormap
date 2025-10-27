package org.tormap.adapter.controller

import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RootController(
    private val buildPropertiesProvider: ObjectProvider<BuildProperties>,

    @Value("\${springdoc.swagger-ui.enabled:false}")
    private val swaggerEnabled: Boolean,

    @Value("\${springdoc.swagger-ui.path:/swagger-ui/index.html}")
    private val swaggerPath: String,
) {

    @GetMapping(path = ["/"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun root(): ResponseEntity<String> {
        val buildProps = buildPropertiesProvider.ifAvailable
        val version = buildProps?.version
            ?: this::class.java.`package`.implementationVersion
            ?: "dev"

        val body = buildString {
            append("TorMap ")
            append(version)
            if (swaggerEnabled) {
                append("\nDocs: ")
                append(swaggerPath)
            }
        }

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .cacheControl(CacheControl.noStore())
            .body(body)
    }
}
