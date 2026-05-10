package org.tormap.controller

import io.kotest.core.spec.style.StringSpec
import org.hamcrest.Matchers.containsString
import org.tormap.database.repository.RelayDetailsRepository
import org.tormap.mockRelayDetails
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(properties = ["app.http.cache.public.max-age-seconds=300"])
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HttpCachingConfigTest(
    private val mockMvc: MockMvc,
    private val relayDetailsRepository: RelayDetailsRepository,
) : StringSpec({
    val testRelay = relayDetailsRepository.save(
        mockRelayDetails().apply {
            familyId = 123L
        }
    )

    "GET /relay/location/days includes ETag header" {
        mockMvc.perform(get("/relay/location/days"))
            .andExpect(status().isOk)
            .andExpect(header().exists("ETag"))
    }

    "GET /relay/details/relay/{id} includes ETag header" {
        mockMvc.perform(get("/relay/details/relay/${testRelay.id}"))
            .andExpect(status().isOk)
            .andExpect(header().exists("ETag"))
    }

    "GET /relay/details/family/{id} includes ETag header" {
        mockMvc.perform(get("/relay/details/family/${testRelay.familyId}"))
            .andExpect(status().isOk)
            .andExpect(header().exists("ETag"))
    }

    "GET /relay/location/day/{day} does not include ETag header" {
        mockMvc.perform(get("/relay/location/day/2022-02-04"))
            .andExpect(status().isOk)
            .andExpect(header().doesNotExist("ETag"))
    }

    "GET /relay/location/days includes Cache-Control public max-age" {
        mockMvc.perform(get("/relay/location/days"))
            .andExpect(status().isOk)
            .andExpect(header().string("Cache-Control", containsString("public")))
            .andExpect(header().string("Cache-Control", containsString("max-age=300")))
    }

    "GET / contains no-store (controller override wins)" {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(header().string("Cache-Control", containsString("no-store")))
            .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.not(containsString("max-age=300"))))
    }

    "GET /openapi does not get caching headers from interceptor" {
        // openapi may be disabled in prod, but is enabled in test/application.yml.
        mockMvc.perform(get("/openapi"))
            .andExpect(status().isOk)
            .andExpect(header().doesNotExist("Cache-Control"))
    }

})
