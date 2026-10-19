package org.tormap.config

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.containsString
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.tormap.adapter.controller.RelayDetailsController
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequestBodyLimitTest(
    private val mockMvc: MockMvc,
    @LocalServerPort private val port: Int,
) : StringSpec({
    val maxBodyBytes = 1024 * 1024

    fun idList(count: Int, firstId: Long = 1L) = (firstId until firstId + count).joinToString(",", "[", "]")

    "body with a Content-Length above the limit returns 413" {
        val body = idList(250_000, firstId = 1_000_000L)
        (body.length > maxBodyBytes) shouldBe true

        mockMvc.perform(
            post("/relay/details/relay/identifiers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isContentTooLarge)
    }

    "chunked body above the limit returns 413" {
        // Whitespace keeps the JSON token count low, so only the body size limit applies. Stays less than
        // server.tomcat.max-swallow-size above the limit, so Tomcat reads the rest and answers cleanly.
        val body = ("[1" + " ".repeat(maxBodyBytes + 256 * 1024) + "]").toByteArray()
        (body.size > maxBodyBytes) shouldBe true

        val request = HttpRequest.newBuilder(URI.create("http://localhost:$port/relay/details/relay/identifiers"))
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            // No known length, so the client uses chunked transfer encoding
            .POST(HttpRequest.BodyPublishers.ofInputStream { ByteArrayInputStream(body) })
            .build()
        val response = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()
            .use { it.send(request, HttpResponse.BodyHandlers.discarding()) }

        response.statusCode() shouldBe 413
    }

    "body within the size limit but with too many JSON tokens is rejected before validation" {
        val body = List(RelayDetailsController.MAXIMUM_EXPECTED_RELAYS_PER_DAY * 2) { 0 }.joinToString(",", "[", "]")
        (body.length < maxBodyBytes) shouldBe true

        mockMvc.perform(
            post("/relay/details/relay/identifiers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string(not(containsString("size must be between"))))
    }

    "largest accepted relay ID list is within the limits" {
        val body = idList(RelayDetailsController.MAXIMUM_EXPECTED_RELAYS_PER_DAY, firstId = 1_000_000_000L)
        (body.length < maxBodyBytes) shouldBe true

        mockMvc.perform(
            post("/relay/details/relay/identifiers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
    }
})
