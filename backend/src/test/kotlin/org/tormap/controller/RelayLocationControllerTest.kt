package org.tormap.controller

import io.kotest.core.spec.style.StringSpec
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RelayLocationControllerTest(
    private val mockMvc: MockMvc,
) : StringSpec({
    "a valid day without relay locations returns an empty list" {
        mockMvc.perform(get("/relay/location/day/2001-01-01"))
            .andExpect(status().isOk)
    }

    listOf("abc", "2024-13-01", "2024-02-30", "2024-5-1", "20240501").forEach { day ->
        "a malformed day '$day' is a bad request instead of a server error" {
            mockMvc.perform(get("/relay/location/day/$day"))
                .andExpect(status().isBadRequest)
        }
    }
})
