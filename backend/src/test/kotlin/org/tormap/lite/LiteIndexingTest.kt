package org.tormap.lite

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDate

class LiteIndexingTest : StringSpec({
    "without a public URL nothing may be crawled" {
        LiteIndexing.robotsTxt(null) shouldBe "User-agent: *\nDisallow: /\n"
    }

    "with a public URL only the start page, the autonomous system pages and the sitemap may be crawled" {
        LiteIndexing.robotsTxt("https://api.example.org") shouldBe """
            User-agent: *
            Allow: /lite/$
            Allow: /lite/as/
            Disallow: /lite/as/*?
            Allow: /lite/sitemap.xml$
            Disallow: /

            Sitemap: https://api.example.org/lite/sitemap.xml

        """.trimIndent()
    }

    "the public URL is validated and loses its trailing slash" {
        LiteIndexing.publicUrl(" https://api.example.org/ ") shouldBe "https://api.example.org"
        LiteIndexing.publicUrl("") shouldBe null
        LiteIndexing.publicUrl(null) shouldBe null
        shouldThrow<IllegalArgumentException> { LiteIndexing.publicUrl("https://api.example.org/\"><x") }
        shouldThrow<IllegalArgumentException> { LiteIndexing.publicUrl("javascript:alert(1)") }
    }

    "the sitemap lists the start page and each autonomous system once" {
        val xml = LiteIndexing.sitemapXml("https://api.example.org", LocalDate.parse("2026-11-11"), listOf(24940, 3, 24940))
        xml shouldContain "<url><loc>https://api.example.org/lite/</loc><lastmod>2026-11-11</lastmod></url>"
        xml shouldContain "<loc>https://api.example.org/lite/as/3</loc>"
        Regex("/lite/as/24940<").findAll(xml).count() shouldBe 1
    }
})
