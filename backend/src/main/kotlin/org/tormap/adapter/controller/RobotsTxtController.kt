package org.tormap.adapter.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.tormap.lite.LiteIndexing
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Serves robots.txt. Crawlers may only index the Lite pages listed by [LiteIndexing], and only if
 * `app.lite.search-indexing` is on. The JSON API is never crawled.
 */
@RestController
class RobotsTxtController(
    @param:Value("\${app.lite.search-indexing:false}")
    searchIndexing: Boolean,
    @param:Value("\${app.public-url:}")
    publicUrl: String,
) {
    private val body = LiteIndexing.robotsTxt(LiteIndexing.indexedUrl(searchIndexing, publicUrl))

    @GetMapping(path = ["/robots.txt"])
    fun robotsTxt(): ResponseEntity<String> =
        ResponseEntity.ok()
            .contentType(MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
            .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
            .body(body)
}
