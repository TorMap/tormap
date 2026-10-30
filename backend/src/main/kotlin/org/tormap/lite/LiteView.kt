package org.tormap.lite

import org.tormap.database.entity.TorRelayFlag
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.round

/**
 * The relay types of the JavaScript map: exits, guards without the exit flag and all others
 */
enum class LiteRelayType(val parameter: String, val label: String) {
    Exit("exit", "Exit"),
    Guard("guard", "Guard"),
    Other("other", "Other");

    companion object {
        /**
         * Returns the type of a relay from its flag ordinals, like getRelayType() in the frontend
         */
        fun of(flags: List<Int>?): LiteRelayType = when {
            flags?.contains(TorRelayFlag.Exit.ordinal) == true -> Exit
            flags?.contains(TorRelayFlag.Guard.ordinal) == true -> Guard
            else -> Other
        }
    }
}

/**
 * The size of the map image. Images are never scaled by the browser, so clicks on them map to exact pixels.
 */
enum class MapSize(val parameter: String, val label: String, val width: Int, val height: Int) {
    Large("large", "Large", 1024, 512),
    Small("small", "Small", 640, 400),
}

/**
 * What a click on the map does: list the relays around the clicked point or zoom in there
 */
enum class ClickMode(val parameter: String, val label: String) {
    List("list", "Show relays"),
    Zoom("zoom", "Zoom in"),
}

/**
 * How relays are grouped on the map, like the "Group relays by" switches of the JavaScript map
 */
enum class Grouping(val parameter: String, val label: String) {
    Family("family", "Family"),
    AutonomousSystem("as", "Autonomous system"),
    Country("country", "Country"),
    Coordinates("coordinates", "Coordinates"),
}

/**
 * Everything that is shown on a Lite page. All values come from the query string, so every view is a link.
 * The map center is stored in pixels of the given zoom level, snapped to a grid and kept inside the world.
 * This keeps the number of distinct map images small, which matters because they are rendered on the server.
 */
data class LiteView(
    val day: LocalDate?,
    val zoom: Int,
    val centerX: Double,
    val centerY: Double,
    val size: MapSize,
    val relayTypes: Set<LiteRelayType>,
    val requiredFlags: Set<TorRelayFlag>,
    val clickMode: ClickMode = ClickMode.List,
    val groupings: Set<Grouping> = emptySet(),
    val selectedFamily: Long? = null,
    val selectedAutonomousSystem: Int? = null,
    /** ISO 3166 alpha-2 code */
    val selectedCountry: String? = null,
    /** Marker size relative to the consensus weight of the relays at a location */
    val sizeByWeight: Boolean = false,
    /** Only the relays with the highest consensus weight, one of [TOP_RELAY_COUNTS], only used with [sizeByWeight] */
    val topRelayCount: Int? = null,
    /** A heatmap of the relays below the markers */
    val heatMap: Boolean = false,
    /** Countries colored by the estimated number of Tor users of the day */
    val countryUsage: Boolean = false,
) {
    val latitude get() = WebMercator.latitude(centerY, zoom)
    val longitude get() = WebMercator.longitude(centerX, zoom)
    val latitudeParameter get() = formatCoordinate(latitude)
    val longitudeParameter get() = formatCoordinate(longitude)

    /** World pixel of the top left corner of the map image */
    val left get() = centerX - size.width / 2.0
    val top get() = centerY - size.height / 2.0

    val canZoomIn get() = zoom < MAX_ZOOM
    val canZoomOut get() = zoom > MIN_ZOOM

    fun withDay(day: LocalDate?) = copy(day = day)

    fun showGroup(grouping: Grouping) = copy(groupings = groupings + grouping)

    fun zoomedBy(delta: Int): LiteView {
        val newZoom = (zoom + delta).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val factor = Math.scalb(1.0, newZoom - zoom)
        return centered(newZoom, centerX * factor, centerY * factor)
    }

    /**
     * Zooms in one level and centers the map on a pixel of the map image
     */
    fun zoomedInAt(imageX: Int, imageY: Int): LiteView {
        val x = left + imageX.coerceIn(0, size.width)
        val y = top + imageY.coerceIn(0, size.height)
        return if (canZoomIn) centered(zoom + 1, x * 2, y * 2) else centered(zoom, x, y)
    }

    /**
     * Moves the map by a fraction of its size, e.g. (0.5, 0) moves it half its width to the east
     */
    fun panned(horizontal: Double, vertical: Double) =
        centered(zoom, centerX + horizontal * size.width, centerY + vertical * size.height)

    private fun centered(zoom: Int, x: Double, y: Double) = copy(
        zoom = zoom,
        centerX = snap(x, zoom, size.width),
        centerY = snap(y, zoom, size.height),
    )

    /**
     * The canonical query string of this view
     */
    fun toQuery(): String = format(queryParameters())

    /**
     * The canonical query string of the map image, which is also its cache key. It leaves out what the image does not
     * depend on, so more pages share an image.
     */
    fun toImageQuery(): String = format(queryParameters().filter { (name, _) -> name != "click" })

    /**
     * The parameters of [toQuery], e.g. for hidden fields of a form
     */
    fun queryParameters(): List<Pair<String, String>> {
        val parameters = mutableListOf<Pair<String, String>>()
        day?.let { parameters += "day" to it.toString() }
        parameters += "lat" to latitudeParameter
        parameters += "lng" to longitudeParameter
        parameters += "zoom" to zoom.toString()
        if (size != MapSize.Large) parameters += "size" to size.parameter
        if (relayTypes != LiteRelayType.entries.toSet()) {
            parameters += "f" to "1"
            LiteRelayType.entries.filter { it in relayTypes }.forEach { parameters += "type" to it.parameter }
        }
        TorRelayFlag.entries.filter { it in requiredFlags }.forEach { parameters += "flag" to it.name }
        Grouping.entries.filter { it in groupings }.forEach { parameters += "group" to it.parameter }
        selectedFamily?.let { parameters += "family" to it.toString() }
        selectedAutonomousSystem?.let { parameters += "as" to it.toString() }
        selectedCountry?.let { parameters += "country" to it }
        if (sizeByWeight) parameters += "weight" to "1"
        topRelayCount?.let { parameters += "top" to it.toString() }
        if (heatMap) parameters += "heat" to "1"
        if (countryUsage) parameters += "usage" to "1"
        if (clickMode != ClickMode.List) parameters += "click" to clickMode.parameter
        return parameters
    }

    // All names and values are dates, numbers or enum names, so they need no URL encoding
    private fun format(parameters: List<Pair<String, String>>) = parameters.joinToString("&") { (name, value) -> "$name=$value" }

    /**
     * The same view in the JavaScript map, which keeps its permalink in the URL hash
     */
    fun toPermalinkHash(): String = buildString {
        append("lat=").append(latitudeParameter)
        append("&lng=").append(longitudeParameter)
        append("&zoom=").append(zoom)
        day?.let { append("&day=").append(it) }
    }

    companion object {
        const val MIN_ZOOM = 2
        const val MAX_ZOOM = 10
        private const val DEFAULT_ZOOM = 2
        private const val DEFAULT_LATITUDE = 30.0
        private const val DEFAULT_LONGITUDE = 0.0
        private const val GRID_PIXELS = 32.0
        val TOP_RELAY_COUNTS = listOf(10, 50, 100, 500, 1000)
        private val countryPattern = Regex("^[A-Z]{2}$")

        /**
         * Builds a view from untrusted query parameters. Invalid values are replaced by defaults instead of failing,
         * like the permalink of the JavaScript map.
         */
        fun parse(parameters: Map<String, List<String>>): LiteView {
            fun first(name: String) = parameters[name]?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
            fun number(name: String, min: Double, max: Double) =
                first(name)?.toDoubleOrNull()?.takeIf { it.isFinite() && it in min..max }

            val zoom = number("zoom", MIN_ZOOM.toDouble(), MAX_ZOOM.toDouble())?.let { round(it).toInt() } ?: DEFAULT_ZOOM
            val latitude = number("lat", -90.0, 90.0)
            val longitude = number("lng", -180.0, 180.0)
            val size = MapSize.entries.find { it.parameter == first("size") } ?: MapSize.Large
            val clickMode = ClickMode.entries.find { it.parameter == first("click") } ?: ClickMode.List
            // Unchecked checkboxes are not submitted, so the form sends f=1 to tell "none checked" from "not sent"
            val relayTypes = if (first("f") != null) {
                val values = parameters["type"].orEmpty().toSet()
                LiteRelayType.entries.filter { it.parameter in values }.toSet()
            } else {
                LiteRelayType.entries.toSet()
            }
            val flagNames = parameters["flag"].orEmpty().toSet()
            val requiredFlags = TorRelayFlag.entries.filter { it.name in flagNames }.toSet()
            val day = first("day")?.let {
                try {
                    LocalDate.parse(it)
                } catch (_: DateTimeParseException) {
                    null
                }
            }
            val groupingNames = parameters["group"].orEmpty().toSet()
            val groupings = Grouping.entries.filter { it.parameter in groupingNames }.toSet()
            val sizeByWeight = first("weight") == "1"
            // Only a few fixed values, so they do not multiply the number of distinct map images
            val topRelayCount = first("top")?.toIntOrNull()?.takeIf { sizeByWeight && it in TOP_RELAY_COUNTS }
            // Latitude and longitude are only used as a pair
            val (centerLatitude, centerLongitude) =
                if (latitude != null && longitude != null) latitude to longitude else DEFAULT_LATITUDE to DEFAULT_LONGITUDE
            return LiteView(
                day = day,
                zoom = zoom,
                centerX = 0.0,
                centerY = 0.0,
                size = size,
                relayTypes = relayTypes,
                requiredFlags = requiredFlags,
                clickMode = clickMode,
                groupings = groupings,
                selectedFamily = first("family")?.toLongOrNull()?.takeIf { it > 0 },
                selectedAutonomousSystem = first("as")?.toIntOrNull()?.takeIf { it > 0 },
                selectedCountry = first("country")?.takeIf(countryPattern::matches),
                sizeByWeight = sizeByWeight,
                topRelayCount = topRelayCount,
                heatMap = first("heat") == "1",
                countryUsage = first("usage") == "1",
            )
                .centered(zoom, WebMercator.x(centerLongitude, zoom), WebMercator.y(centerLatitude, zoom))
        }

        private fun snap(value: Double, zoom: Int, viewSize: Int): Double {
            val worldSize = WebMercator.worldSize(zoom)
            if (worldSize <= viewSize) return worldSize / 2
            return (round(value / GRID_PIXELS) * GRID_PIXELS).coerceIn(viewSize / 2.0, worldSize - viewSize / 2.0)
        }

        private fun formatCoordinate(value: Double) = String.format(Locale.ROOT, "%.4f", value)
    }
}
