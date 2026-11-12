package org.tormap.lite

import java.time.LocalDate

/**
 * What search engines and AI crawlers may index: only the Lite start page (latest day) and the pages of autonomous
 * systems, without a query string. Those are a few thousand light pages with real content. Everything else (other
 * days and views, relay and family pages, search, the rendered images) is disallowed, so crawlers can't make the
 * backend render millions of variants.
 */
object LiteIndexing {
    const val SITEMAP_PATH = "/lite/sitemap.xml"
    private val plainUrl = Regex("^https?://[^\\s\"'<>]+$")

    /**
     * Validates the configured public URL of the backend and removes a trailing slash.
     * @return null if no URL is configured, which keeps the whole backend out of search engines
     */
    fun publicUrl(configured: String?): String? {
        val url = configured?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() } ?: return null
        require(plainUrl.matches(url)) { "Invalid public URL: $url" }
        return url
    }

    /**
     * robots.txt (RFC 9309). The longest matching rule wins, so `/lite/as/123` is allowed and `/lite/as/123?day=…`
     * is not. Without a public URL nothing may be crawled, e.g. on self-hosted instances.
     */
    fun robotsTxt(publicUrl: String?): String = if (publicUrl == null) {
        "User-agent: *\nDisallow: /\n"
    } else {
        listOf(
            "User-agent: *",
            "Allow: ${LitePage.PATH}$",
            "Allow: ${LitePage.AUTONOMOUS_SYSTEM_PATH}",
            "Disallow: ${LitePage.AUTONOMOUS_SYSTEM_PATH}*?",
            "Allow: $SITEMAP_PATH$",
            "Disallow: /",
            "",
            "Sitemap: $publicUrl$SITEMAP_PATH",
            "",
        ).joinToString("\n")
    }

    /**
     * The sitemap of the indexable pages of the latest day
     * @param autonomousSystems - The numbers of the autonomous systems with relays on that day
     */
    fun sitemapXml(publicUrl: String, day: LocalDate?, autonomousSystems: Collection<Int>): String = buildString {
        val lastModified = day?.let { "<lastmod>$it</lastmod>" } ?: ""
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n")
        append("<url><loc>${escapeHtml(publicUrl + LitePage.PATH)}</loc>$lastModified</url>\n")
        autonomousSystems.distinct().sorted().forEach {
            append("<url><loc>${escapeHtml("$publicUrl${LitePage.AUTONOMOUS_SYSTEM_PATH}$it")}</loc>$lastModified</url>\n")
        }
        append("</urlset>\n")
    }
}
