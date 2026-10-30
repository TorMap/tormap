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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import org.tormap.adapter.controller.exception.RelayNotFoundException
import org.tormap.config.CacheConfig
import org.tormap.database.repository.RelayDetailsRepository
import org.tormap.adapter.dto.RelayLocationDto
import org.tormap.lite.ClickMode
import org.tormap.lite.CountryMeta
import org.tormap.lite.Grouping
import org.tormap.lite.LiteAggregation
import org.tormap.lite.Ranking
import org.tormap.lite.RankingRow
import org.tormap.lite.LiteMapRenderer
import org.tormap.lite.LitePage
import org.tormap.lite.LitePageModel
import org.tormap.lite.LiteRelayPages
import org.tormap.lite.LiteRelays
import org.tormap.lite.LiteView
import org.tormap.service.RelayDetailsQueryService
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * TorMap Lite: the map as server-rendered HTML and PNG for browsers without JavaScript (e.g. Tor Browser "Safest").
 * Every page is a GET form. Buttons and map clicks submit an action (`go`, `map.x`/`map.y`), which is applied and
 * answered with a redirect to the canonical URL of the resulting view, so the address bar always holds a clean link.
 * A click lists the relays at that point (`x`/`y`) or zooms in, depending on the view's click mode. Relays, families
 * and autonomous systems have their own pages, which keep the view's query so they can link back to the same map.
 */
@Controller
class LiteController(
    private val relayLocationController: RelayLocationController,
    private val relayDetailsQueryService: RelayDetailsQueryService,
    private val relayDetailsRepository: RelayDetailsRepository,
    private val liteMapRenderer: LiteMapRenderer,
    private val countryMeta: CountryMeta,
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
                else -> if (mapX != null && mapY != null && view.clickMode == ClickMode.Zoom) view.zoomedInAt(mapX, mapY) else view
            }
            // A click that lists relays keeps the view and adds the clicked pixel, the list follows below the map
            val pick = if (action == null && mapX != null && mapY != null && view.clickMode == ClickMode.List) {
                "&x=${mapX.coerceIn(0, view.size.width)}&y=${mapY.coerceIn(0, view.size.height)}#relays"
            } else {
                ""
            }
            return redirect("${LitePage.PATH}?${next.toQuery()}$pick")
        }
        val pickX = parameters.getFirst("x")?.toIntOrNull()?.takeIf { it in 0..view.size.width }
        val pickY = parameters.getFirst("y")?.toIntOrNull()?.takeIf { it in 0..view.size.height }

        val relays = relaysOf(day)
        val filtered = LiteAggregation.filterRelays(relays, view)
        val shownView = LiteAggregation.normalized(view, filtered)
        val notice = when {
            days.isEmpty() -> "No relay data has been imported yet."
            parsed.day != null && parsed.day != day -> "There is no relay data for ${parsed.day}, showing $day instead."
            else -> null
        }
        val model = LitePageModel(
            view = shownView,
            firstDay = days.firstOrNull(),
            lastDay = days.lastOrNull(),
            hasPreviousDay = day != null && day > days.first(),
            hasNextDay = day != null && day < days.last(),
            shownRelayCount = filtered.size,
            totalRelayCount = relays.size,
            notice = notice,
            fullVersionUrl = fullVersionUrl(),
            pickedRelays = if (pickX != null && pickY != null) LiteRelays.near(filtered, shownView, pickX, pickY) else null,
            statistics = LiteAggregation.statistics(filtered, shownView),
            rankings = rankings(filtered, shownView),
        )
        return html(LitePage.render(model))
    }

    @GetMapping("${LitePage.RELAY_PATH}{id}")
    fun relay(@PathVariable id: Long, @RequestParam parameters: MultiValueMap<String, String>): ResponseEntity<String> {
        val view = resolvedView(parameters)
        val details = try {
            relayDetailsQueryService.getRelay(id)
        } catch (_: RelayNotFoundException) {
            return notFound("There is no relay with the ID $id.", view)
        }
        val location = relaysOf(view.day).find { it.detailsId == id }
        return html(LiteRelayPages.renderRelay(details, location, view, fullVersionUrl()))
    }

    @GetMapping("${LitePage.FAMILY_PATH}{id}")
    fun family(@PathVariable id: Long, @RequestParam parameters: MultiValueMap<String, String>): ResponseEntity<String> {
        val view = resolvedView(parameters)
        val members = relayDetailsRepository.findAllByFamilyId(id)
        if (members.isEmpty()) return notFound("There is no family with the ID $id.", view)
        return html(LiteRelayPages.renderFamily(id, members, view, fullVersionUrl()))
    }

    @GetMapping("${LitePage.AUTONOMOUS_SYSTEM_PATH}{number}")
    fun autonomousSystem(@PathVariable number: Int, @RequestParam parameters: MultiValueMap<String, String>): ResponseEntity<String> {
        val view = resolvedView(parameters)
        val relays = LiteAggregation.filterRelays(relaysOf(view.day), view).filter { it.asNumber == number }
        // The name is taken from the details of one relay instead of grouping all relays of the month
        val name = relays.firstNotNullOfOrNull { it.detailsId }
            ?.let { relayDetailsRepository.findById(it).orElse(null)?.autonomousSystemName }
        return html(LiteRelayPages.renderAutonomousSystem(number, name, relays, view, fullVersionUrl()))
    }

    @GetMapping(LitePage.SEARCH_PATH)
    fun search(@RequestParam parameters: MultiValueMap<String, String>): ResponseEntity<String> {
        val view = resolvedView(parameters)
        val query = parameters.getFirst("q")?.trim()?.take(LiteRelays.MAX_QUERY_LENGTH).orEmpty()
        if (query.isEmpty()) return redirect("${LitePage.PATH}?${view.toQuery()}")
        val day = view.day
        // A fingerprint is looked up once in the relay details of the day's month
        val detailsIds = if (day != null && LiteRelays.isFingerprint(query)) {
            setOfNotNull(relayDetailsRepository.findByMonthAndFingerprint(YearMonth.from(day).toString(), query.uppercase())?.id)
        } else {
            emptySet()
        }
        val relays = LiteRelays.search(LiteAggregation.filterRelays(relaysOf(day), view), query, detailsIds)
        return html(LiteRelayPages.renderSearch(query, relays, view, fullVersionUrl()))
    }

    @GetMapping(LitePage.MAP_PATH)
    fun mapImage(@RequestParam parameters: MultiValueMap<String, String>): ResponseEntity<ByteArray> {
        val parsed = LiteView.parse(parameters)
        // Only days with data are looked up, so arbitrary days do not reach the database or evict cached days
        val day = parsed.day?.takeIf { it in relayLocationController.getDays() }
        // Selections of groups without relays are dropped, so made-up IDs do not create more distinct images
        val view = LiteAggregation.normalized(parsed.withDay(day), LiteAggregation.filterRelays(relaysOf(day), parsed))
        val image = try {
            mapImageCache.get(view.toImageQuery()) { render(view) }
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

    /**
     * The largest groups of each grouping that is shown, with links to select them on the map
     */
    private fun rankings(relays: List<RelayLocationDto>, view: LiteView): List<Ranking> {
        val query = view.toQuery()
        val rankings = mutableListOf<Ranking>()
        if (Grouping.Family in view.groupings) {
            rankings += Ranking("Largest families", LiteAggregation.largestGroups(relays, { it.familyId }, RANKING_SIZE).map { (id, count) ->
                RankingRow("Family $id", count, view.copy(selectedFamily = id as Long), "${LitePage.FAMILY_PATH}$id?$query")
            })
        }
        if (Grouping.AutonomousSystem in view.groupings) {
            rankings += Ranking("Largest autonomous systems", LiteAggregation.largestGroups(relays, { it.asNumber }, RANKING_SIZE).map { (number, count) ->
                RankingRow("AS$number", count, view.copy(selectedAutonomousSystem = number as Int), "${LitePage.AUTONOMOUS_SYSTEM_PATH}$number?$query")
            })
        }
        if (Grouping.Country in view.groupings) {
            rankings += Ranking("Countries with the most relays", LiteAggregation.largestGroups(relays, { it.country }, RANKING_SIZE).map { (country, count) ->
                RankingRow(countryMeta.name(country as String), count, view.copy(selectedCountry = country), null)
            })
        }
        return rankings
    }

    /** The view of the query with a day that has data */
    private fun resolvedView(parameters: MultiValueMap<String, String>): LiteView {
        val parsed = LiteView.parse(parameters)
        return parsed.withDay(resolveDay(availableDays(), parsed.day))
    }

    private fun fullVersionUrl() = fullVersionUrl.trimEnd('/')

    private fun html(body: String, status: HttpStatus = HttpStatus.OK): ResponseEntity<String> = ResponseEntity.status(status)
        .contentType(MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
        .body(body)

    private fun notFound(message: String, view: LiteView) =
        html(LiteRelayPages.renderNotFound(message, view, fullVersionUrl()), HttpStatus.NOT_FOUND)

    private fun redirect(location: String): ResponseEntity<String> =
        ResponseEntity.status(HttpStatus.SEE_OTHER).header(HttpHeaders.LOCATION, location).build()

    private fun relaysOf(day: LocalDate?) = day?.let { relayLocationController.getDay(it.toString()) } ?: emptyList()

    companion object {
        private const val PAN_FRACTION = 0.5
        private const val RANKING_SIZE = 10
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
