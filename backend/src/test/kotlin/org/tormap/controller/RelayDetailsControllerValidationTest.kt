package org.tormap.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.StringSpec
import org.hamcrest.Matchers.containsString
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.tormap.adapter.controller.RelayDetailsController

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RelayDetailsControllerValidationTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : StringSpec({

    "family identifiers - valid list returns 200" {
        val payload = listOf(1L)
        mockMvc.perform(
            post("/relay/details/family/identifiers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
        )
            .andExpect(status().isOk)
    }

    "family identifiers - empty list returns 400 with size error" {
        val payload: List<Long> = emptyList()
        mockMvc.perform(
            post("/relay/details/family/identifiers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string(containsString("size must be between 1 and ${RelayDetailsController.MAXIMUM_EXPECTED_FAMILIES_PER_MONTH}")))
    }

    "family identifiers - too large list returns 400 with size error" {
        val payload = (1L..RelayDetailsController.MAXIMUM_EXPECTED_FAMILIES_PER_MONTH + 1).toList()
        mockMvc.perform(
            post("/relay/details/family/identifiers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string(containsString("size must be between 1 and ${RelayDetailsController.MAXIMUM_EXPECTED_FAMILIES_PER_MONTH}")))
    }

    "relay identifiers - empty list returns 400 with size error" {
        val payload: List<Long> = emptyList()
        mockMvc.perform(
            post("/relay/details/relay/identifiers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string(containsString("size must be between 1 and ${RelayDetailsController.MAXIMUM_EXPECTED_RELAYS_PER_DAY}")))
    }

    "relay identifiers - too large list returns 400 with size error" {
        val payload = (1L..RelayDetailsController.MAXIMUM_EXPECTED_RELAYS_PER_DAY + 1).toList()
        mockMvc.perform(
            post("/relay/details/relay/identifiers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string(containsString("size must be between 1 and ${RelayDetailsController.MAXIMUM_EXPECTED_RELAYS_PER_DAY}")))
    }

})
