package org.tormap.config

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options

/**
 * Verifies the access rules of [SecurityConfig] over HTTP.
 * Public paths may answer 404 (nothing mapped), but must never be rejected by security (401/403).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = ["springdoc.swagger-ui.enabled=false", "springdoc.api-docs.enabled=false"])
class SecurityRulesTest(
    private val mockMvc: MockMvc,
    @Value("\${app.security.cors.allowed-origins}") private val allowedOrigins: String,
) : StringSpec({

    fun statusOf(path: String) = mockMvc.perform(get(path)).andReturn().response.status

    "public paths are not rejected by security" {
        listOf("/", "/error", "/favicon.ico", "/robots.txt", "/static/missing.txt", "/relay/missing").forEach { path ->
            statusOf(path) shouldNotBe 401
            statusOf(path) shouldNotBe 403
        }
    }

    "actuator endpoints require authentication" {
        statusOf("/actuator/health") shouldBe 401
        statusOf("/actuator/metrics") shouldBe 401
    }

    "other paths require authentication" {
        statusOf("/not-a-public-path") shouldBe 401
    }

    "swagger and openapi are not public when disabled" {
        listOf("/swagger-ui.html", "/swagger-ui/index.html", "/v3/api-docs", "/openapi/api.yaml").forEach { path ->
            statusOf(path) shouldBe 401
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
