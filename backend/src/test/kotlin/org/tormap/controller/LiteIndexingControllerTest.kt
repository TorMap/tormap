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

@SpringBootTest(properties = ["app.lite.public-url=https://api.example.org/"])
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LiteIndexingControllerTest(
    private val mockMvc: MockMvc,
) : StringSpec({
    "robots.txt allows the indexable Lite pages and names the sitemap" {
        val robots = mockMvc.perform(get("/robots.txt")).andReturn().response.contentAsString
        robots shouldContain "Allow: /lite/$"
        robots shouldContain "Sitemap: https://api.example.org/lite/sitemap.xml"
    }

    "the sitemap is XML with the start page" {
        val response = mockMvc.perform(get("/lite/sitemap.xml")).andReturn().response
        response.status shouldBe 200
        response.contentType shouldStartWith "application/xml"
        response.contentAsString shouldContain "<loc>https://api.example.org/lite/</loc>"
    }

    "only pages without a query string have a canonical link" {
        mockMvc.perform(get("/lite/")).andReturn().response.contentAsString shouldContain
            "<link rel=\"canonical\" href=\"https://api.example.org/lite/\">"
        mockMvc.perform(get("/lite/as/3320")).andReturn().response.contentAsString shouldContain
            "<link rel=\"canonical\" href=\"https://api.example.org/lite/as/3320\">"
        mockMvc.perform(get("/lite/?zoom=4")).andReturn().response.contentAsString shouldNotContain "rel=\"canonical\""
        mockMvc.perform(get("/lite/as/3320?zoom=4")).andReturn().response.contentAsString shouldNotContain "rel=\"canonical\""
    }

    "autonomous system pages have a descriptive title and description" {
        val html = mockMvc.perform(get("/lite/as/3320")).andReturn().response.contentAsString
        html shouldContain "<title>Tor relays in Autonomous system (AS3320) - TorMap Lite</title>"
        html shouldContain "<meta name=\"description\" content=\"0 Tor relays in Autonomous system (AS3320) on"
    }
})
