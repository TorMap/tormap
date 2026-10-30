package org.tormap.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.tormap.adapter.controller.LiteController
import org.tormap.database.repository.RelayDetailsRepository
import org.tormap.mockRelayDetails
import java.io.ByteArrayInputStream
import java.time.LocalDate
import javax.imageio.ImageIO

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LiteControllerTest(
    private val mockMvc: MockMvc,
    private val relayDetailsRepository: RelayDetailsRepository,
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

    "a click on the map zooms in when the click mode is zoom" {
        val response = mockMvc.perform(get("/lite/?lat=0&lng=0&zoom=3&click=zoom&map.x=512&map.y=256")).andReturn().response
        response.status shouldBe 303
        response.getHeader("Location") shouldContain "zoom=4"
    }

    "a click on the map lists the relays there by default" {
        val response = mockMvc.perform(get("/lite/?lat=0&lng=0&zoom=3&map.x=100&map.y=50")).andReturn().response
        response.status shouldBe 303
        response.getHeader("Location") shouldBe "/lite/?lat=0.0000&lng=0.0000&zoom=3&x=100&y=50#relays"
        val page = mockMvc.perform(get("/lite/?lat=0&lng=0&zoom=3&x=100&y=50")).andReturn().response.contentAsString
        page shouldContain "Relays at the clicked point"
    }

    "relay pages escape relay data and link back to the map" {
        val relay = relayDetailsRepository.save(mockRelayDetails('E').apply {
            nickname = "<i>x"
            contact = "javascript:alert(1) <script>alert(1)</script>"
            familyId = 4711L
        })
        val response = mockMvc.perform(get("/lite/relay/${relay.id}?zoom=4")).andReturn().response
        response.status shouldBe 200
        val html = response.contentAsString
        html shouldContain "&lt;i&gt;x"
        html shouldContain "javascript:alert(1) &lt;script&gt;alert(1)&lt;/script&gt;"
        html shouldNotContain "<i>x"
        html shouldNotContain "<script"
        html shouldNotContain "href=\"javascript"
        html shouldContain "href=\"https://metrics.torproject.org/rs.html#details/${"E".repeat(40)}\""
        html shouldContain "href=\"/lite/?lat="

        val family = mockMvc.perform(get("/lite/family/4711")).andReturn().response
        family.status shouldBe 200
        family.contentAsString shouldContain "/lite/relay/${relay.id}?"
    }

    "unknown relays and families are HTML 404 pages" {
        listOf("/lite/relay/999999999", "/lite/family/999999999").forEach { path ->
            val response = mockMvc.perform(get(path)).andReturn().response
            response.status shouldBe 404
            response.contentType shouldStartWith "text/html"
        }
    }

    "autonomous system and search pages work without data" {
        mockMvc.perform(get("/lite/as/3320")).andReturn().response.status shouldBe 200
        mockMvc.perform(get("/lite/search?q=relay")).andReturn().response.contentAsString shouldContain "Relays matching"
        val emptySearch = mockMvc.perform(get("/lite/search?q=&zoom=4")).andReturn().response
        emptySearch.status shouldBe 303
        emptySearch.getHeader("Location") shouldContain "zoom=4"
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

    "the page shows statistics and the grouping settings" {
        val html = mockMvc.perform(get("/lite/?group=family&group=country&weight=1&top=50")).andReturn().response.contentAsString
        html shouldContain "Shown relays"
        html shouldContain "Group relays by"
        html shouldContain "name=\"group\" value=\"family\" checked"
        html shouldContain "group=family&amp;group=country&amp;weight=1&amp;top=50"
    }

    "selections of groups without relays on the day are dropped" {
        val html = mockMvc.perform(get("/lite/?group=family&family=4711&country=DE&as=3320")).andReturn().response.contentAsString
        html shouldNotContain "Selected:"
        html shouldNotContain "family=4711"
    }

    "the map is drawn with all groupings" {
        val response = mockMvc.perform(get("/lite/map.png?group=family&group=as&group=country&group=coordinates&weight=1&top=10&country=DE"))
            .andReturn().response
        response.status shouldBe 200
        ImageIO.read(ByteArrayInputStream(response.contentAsByteArray)).width shouldBe 1024
    }

    "days without data resolve to the closest earlier day" {
        val days = listOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5))
        LiteController.resolveDay(days, null) shouldBe LocalDate.of(2024, 1, 5)
        LiteController.resolveDay(days, LocalDate.of(2024, 1, 3)) shouldBe LocalDate.of(2024, 1, 1)
        LiteController.resolveDay(days, LocalDate.of(2023, 1, 1)) shouldBe LocalDate.of(2024, 1, 1)
        LiteController.resolveDay(emptyList(), LocalDate.of(2023, 1, 1)) shouldBe null
    }
})
