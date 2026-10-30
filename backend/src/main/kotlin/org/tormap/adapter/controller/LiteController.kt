package org.tormap.adapter.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import org.tormap.config.CacheConfig
import org.tormap.lite.LiteMapRenderer
import org.tormap.lite.LitePage
import org.tormap.lite.LitePageModel
import org.tormap.lite.LiteView
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * TorMap Lite: the map as server-rendered HTML and PNG for browsers without JavaScript (e.g. Tor Browser "Safest").
 * Every page is a GET form. Buttons and map clicks submit an action (`go`, `map.x`/`map.y`), which is applied and
 * answered with a redirect to the canonical URL of the resulting view, so the address bar always holds a clean link.
 */
@Controller
class LiteController(
    private val relayLocationController: RelayLocationController,
    private val liteMapRenderer: LiteMapRenderer,
    cacheManager: CacheManager,
    @param:Value("\${app.lite.full-version-url:https://tormap.org}")
    private val fullVersionUrl: String,
) {
    private val mapImageCache: Cache = requireNotNull(cacheManager.getCache(CacheConfig.LITE_MAP_IMAGES))

    // Rendering takes CPU time, so only a few images are rendered at once and further requests wait briefly
    private val renderPermits = Semaphore(maxOf(1, Runtime.getRuntime().availableProcessors() / 2))

    @GetMapping("/lite")
    fun redirectToPage(): ResponseEntity<Void> =
        ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).header(HttpHeaders.LOCATION, LitePage.PATH).build()

    @GetMapping(LitePage.PATH)
    fun page(@RequestParam parameters: MultiValueMap<String, String>): ResponseEntity<String> {
        val days = availableDays()
        val parsed = LiteView.parse(parameters)
        val day = resolveDay(days, parsed.day)
        val view = parsed.withDay(day)

        val action = parameters.getFirst("go")
        val mapX = parameters.getFirst("map.x")?.toIntOrNull()
        val mapY = parameters.getFirst("map.y")?.toIntOrNull()
        if (action != null || (mapX != null && mapY != null)) {
            val next = when (action) {
                "previous" -> view.withDay(day?.let { current -> days.lastOrNull { it < current } } ?: day)
                "next" -> view.withDay(day?.let { current -> days.firstOrNull { it > current } } ?: day)
                "latest" -> view.withDay(days.lastOrNull())
                "in" -> view.zoomedBy(1)
                "out" -> view.zoomedBy(-1)
                "north" -> view.panned(0.0, -PAN_FRACTION)
                "south" -> view.panned(0.0, PAN_FRACTION)
                "west" -> view.panned(-PAN_FRACTION, 0.0)
                "east" -> view.panned(PAN_FRACTION, 0.0)
                else -> if (mapX != null && mapY != null) view.zoomedInAt(mapX, mapY) else view
            }
            return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .header(HttpHeaders.LOCATION, "${LitePage.PATH}?${next.toQuery()}")
                .build()
        }

        val relays = relaysOf(day)
        val notice = when {
            days.isEmpty() -> "No relay data has been imported yet."
            parsed.day != null && parsed.day != day -> "There is no relay data for ${parsed.day}, showing $day instead."
            else -> null
        }
        val model = LitePageModel(
            view = view,
            firstDay = days.firstOrNull(),
            lastDay = days.lastOrNull(),
            hasPreviousDay = day != null && day > days.first(),
            hasNextDay = day != null && day < days.last(),
            shownRelayCount = liteMapRenderer.filterRelays(relays, view).size,
            totalRelayCount = relays.size,
            notice = notice,
            fullVersionUrl = fullVersionUrl.trimEnd('/'),
        )
        return ResponseEntity.ok()
            .contentType(MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
            .body(LitePage.render(model))
    }

    @GetMapping(LitePage.MAP_PATH)
    fun mapImage(@RequestParam parameters: MultiValueMap<String, String>): ResponseEntity<ByteArray> {
        val parsed = LiteView.parse(parameters)
        // Only days with data are looked up, so arbitrary days do not reach the database or evict cached days
        val view = parsed.withDay(parsed.day?.takeIf { it in relayLocationController.getDays() })
        val image = try {
            mapImageCache.get(view.toQuery()) { render(view) }
        } catch (exception: Cache.ValueRetrievalException) {
            throw exception.cause as? ResponseStatusException ?: exception
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(image)
    }

    private fun render(view: LiteView): ByteArray {
        if (!renderPermits.tryAcquire(RENDER_WAIT_SECONDS, TimeUnit.SECONDS)) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Too many map images are being drawn. Please try again.")
        }
        try {
            return liteMapRenderer.render(view, relaysOf(view.day))
        } finally {
            renderPermits.release()
        }
    }

    private fun availableDays(): List<LocalDate> = relayLocationController.getDays().sorted()

    private fun relaysOf(day: LocalDate?) = day?.let { relayLocationController.getDay(it.toString()) } ?: emptyList()

    companion object {
        private const val PAN_FRACTION = 0.5
        private const val RENDER_WAIT_SECONDS = 5L

        /**
         * Returns the requested day if it has data, otherwise the closest earlier day with data (or the first day).
         * Without a requested day, the latest day is returned.
         */
        internal fun resolveDay(days: List<LocalDate>, requested: LocalDate?): LocalDate? {
            if (days.isEmpty()) return null
            if (requested == null) return days.last()
            return days.lastOrNull { it <= requested } ?: days.first()
        }
    }
}
