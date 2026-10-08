package org.tormap.config

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

/** Swagger and OpenAPI paths are only public when explicitly enabled. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = ["springdoc.swagger-ui.enabled=true", "springdoc.api-docs.enabled=true"])
class SecuritySwaggerEnabledTest(
    private val mockMvc: MockMvc,
) : StringSpec({

    "swagger and openapi are public when enabled" {
        listOf("/openapi", "/swagger", "/swagger-ui/index.html").forEach { path ->
            mockMvc.perform(get(path)).andReturn().response.status shouldNotBe 401
        }
    }

})
