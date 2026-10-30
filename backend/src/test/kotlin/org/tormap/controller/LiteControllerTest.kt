package org.tormap.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.tormap.adapter.controller.LiteController
import java.io.ByteArrayInputStream
import java.time.LocalDate
import javax.imageio.ImageIO

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LiteControllerTest(
    private val mockMvc: MockMvc,
) : StringSpec({
    "the page is HTML with the map, the day controls and a link to the JavaScript version" {
        val response = mockMvc.perform(get("/lite/")).andReturn().response
        response.status shouldBe 200
        response.contentType shouldStartWith "text/html"
        val html = response.contentAsString
        html shouldStartWith "<!DOCTYPE html>"
        html shouldContain "type=\"image\" name=\"map\" src=\"/lite/map.png?"
        html shouldContain "name=\"go\" value=\"previous\""
        html shouldContain "type=\"date\""
        html shouldContain "href=\"https://tormap.org/#lat="
        html.contains("<script") shouldBe false
    }

    "/lite redirects to /lite/" {
        val response = mockMvc.perform(get("/lite")).andReturn().response
        response.status shouldBe 301
        response.getHeader("Location") shouldBe "/lite/"
    }

    "a button redirects to the canonical URL of the resulting view" {
        val response = mockMvc.perform(get("/lite/?lat=0&lng=0&zoom=3&go=in")).andReturn().response
        response.status shouldBe 303
        response.getHeader("Location") shouldBe "/lite/?lat=0.0000&lng=0.0000&zoom=4"
    }

    "a click on the map zooms in" {
        val response = mockMvc.perform(get("/lite/?lat=0&lng=0&zoom=3&map.x=512&map.y=256")).andReturn().response
        response.status shouldBe 303
        response.getHeader("Location") shouldContain "zoom=4"
    }

    "malformed parameters do not fail" {
        mockMvc.perform(get("/lite/?day=x&lat=y&zoom=z&go=nowhere&map.x=a")).andReturn().response.status shouldBe 303
        mockMvc.perform(get("/lite/?day=x&lat=y&zoom=z")).andReturn().response.status shouldBe 200
    }

    "the map is a PNG of the requested size" {
        val response = mockMvc.perform(get("/lite/map.png?size=small&day=2001-01-01")).andReturn().response
        response.status shouldBe 200
        response.contentType shouldBe "image/png"
        val image = ImageIO.read(ByteArrayInputStream(response.contentAsByteArray))
        image.width shouldBe 640
        image.height shouldBe 400
    }

    "days without data resolve to the closest earlier day" {
        val days = listOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5))
        LiteController.resolveDay(days, null) shouldBe LocalDate.of(2024, 1, 5)
        LiteController.resolveDay(days, LocalDate.of(2024, 1, 3)) shouldBe LocalDate.of(2024, 1, 1)
        LiteController.resolveDay(days, LocalDate.of(2023, 1, 1)) shouldBe LocalDate.of(2024, 1, 1)
        LiteController.resolveDay(emptyList(), LocalDate.of(2023, 1, 1)) shouldBe null
    }
})
