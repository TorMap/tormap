package org.tormap.config

import io.kotest.core.spec.style.StringSpec
import jakarta.servlet.http.Cookie
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import java.util.Base64

/**
 * Verifies the access rules of [SecurityConfig] over HTTP.
 * Public paths may answer 404 (nothing mapped), but must never be rejected by security (401/403).
 */
@SpringBootTest
@AutoConfigureMockMvc
// No dev or prod profile, so these rules check the defaults from application.yml
@ActiveProfiles("test")
class SecurityRulesTest(
    private val mockMvc: MockMvc,
    @param:Value("\${app.security.cors.allowed-origins}") private val allowedOrigins: String,
) : StringSpec({

    fun statusOf(path: String) = mockMvc.perform(get(path)).andReturn().response.status

    "public paths are not rejected by security" {
        listOf("/", "/error", "/favicon.ico", "/robots.txt", "/static/missing.txt", "/relay/missing", "/relay/country-usage/day/2024-01-01").forEach { path ->
            statusOf(path) shouldNotBe 401
            statusOf(path) shouldNotBe 403
        }
    }

    "public paths ignore HTTP Basic credentials" {
        val basicAuth = "Basic " + Base64.getEncoder().encodeToString("admin:wrong".toByteArray())
        listOf("/", "/relay/missing").forEach { path ->
            val response = mockMvc.perform(get(path).header("Authorization", basicAuth)).andReturn().response
            response.status shouldNotBe 401
            response.getHeader("WWW-Authenticate") shouldBe null
        }
    }

    "actuator endpoints require authentication" {
        statusOf("/actuator/health") shouldBe 401
        statusOf("/actuator/metrics") shouldBe 401
    }

    "actuator asks browsers for HTTP Basic credentials" {
        mockMvc.perform(get("/actuator/health")).andReturn().response.getHeader("WWW-Authenticate") shouldStartWith "Basic"
    }

    "actuator rejects unsafe methods without CSRF token before authentication" {
        mockMvc.perform(post("/actuator/loggers/ROOT")).andReturn().response.status shouldBe 403
    }

    "actuator accepts a matching CSRF cookie and header, then requires authentication" {
        val status = mockMvc.perform(
            post("/actuator/loggers/ROOT")
                .cookie(Cookie("XSRF-TOKEN", "token"))
                .header("X-XSRF-TOKEN", "token")
        ).andReturn().response.status
        status shouldBe 401
    }

    "public API POST is not subject to CSRF" {
        val status = mockMvc.perform(post("/relay/missing")).andReturn().response.status
        status shouldNotBe 403
        status shouldNotBe 401
    }

    "other paths are denied" {
        statusOf("/not-a-public-path") shouldBe 403
    }

    "content security policy is sent by default" {
        mockMvc.perform(get("/")).andReturn().response.getHeader("Content-Security-Policy") shouldStartWith "default-src 'none'"
    }

    "swagger and openapi are not public by default" {
        listOf("/swagger-ui.html", "/swagger-ui/index.html", "/v3/api-docs", "/openapi/api.yaml").forEach { path ->
            statusOf(path) shouldBe 403
        }
    }

    "CORS preflight is allowed for the configured origin" {
        val origin = allowedOrigins.split(',').first().trim()
        val response = mockMvc.perform(
            options("/relay/location/days")
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "GET")
        ).andReturn().response
        response.status shouldBe 200
        response.getHeader("Access-Control-Allow-Origin") shouldBe origin
    }

})
