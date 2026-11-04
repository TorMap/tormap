package org.tormap.adapter.controller

import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Serves security.txt (RFC 9116), which tells security researchers how to report a vulnerability. The contact is the
 * GitHub advisory form described in SECURITY.md. It stays valid as long as the project exists, so the expiry date
 * moves along instead of being renewed by hand.
 */
@RestController
class SecurityTxtController {

    companion object {
        const val PATH = "/.well-known/security.txt"

        // RFC 9116 recommends an expiry date less than a year in the future
        private val VALIDITY: Duration = Duration.ofDays(180)

        fun securityTxt(now: Instant): String {
            val expires = now.truncatedTo(ChronoUnit.DAYS).plus(VALIDITY)
            return """
                Contact: https://github.com/TorMap/tormap/security/advisories/new
                Expires: $expires
                Policy: https://github.com/TorMap/tormap/security/policy
                Preferred-Languages: en
            """.trimIndent() + "\n"
        }
    }

    @GetMapping(path = [PATH])
    fun securityTxt(): ResponseEntity<String> =
        ResponseEntity.ok()
            .contentType(MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
            .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
            .body(securityTxt(Instant.now()))
}
