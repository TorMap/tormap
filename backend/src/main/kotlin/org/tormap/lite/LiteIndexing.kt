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
     * The base URL of the indexable pages, from `app.lite.search-indexing` and `app.public-url`
     * @return null if indexing is off, which keeps the whole backend out of search engines
     * @throws IllegalStateException if indexing is on without a public URL, so a half-configured deployment fails
     * at startup instead of silently staying out of search engines
     * @throws IllegalArgumentException if the public URL is not a plain http(s) URL
     */
    fun indexedUrl(searchIndexing: Boolean, publicUrl: String?): String? {
        if (!searchIndexing) return null
        val url = publicUrl?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }
            ?: error("app.lite.search-indexing (TORMAP_LITE_SEARCH_INDEXING) needs app.public-url (TORMAP_PUBLIC_URL)")
        require(plainUrl.matches(url)) { "Invalid public URL: $url" }
        return url
    }

    /**
     * robots.txt (RFC 9309). The longest matching rule wins, so `/lite/as/123` is allowed and `/lite/as/123?day=…`
     * is not. Without indexing nothing may be crawled, e.g. on self-hosted instances.
     * @param indexedUrl - See [indexedUrl]
     */
    fun robotsTxt(indexedUrl: String?): String = if (indexedUrl == null) {
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
            "Sitemap: $indexedUrl$SITEMAP_PATH",
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
