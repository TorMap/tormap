package org.tormap.lite

import org.springframework.stereotype.Component
import org.tormap.adapter.dto.RelayLocationDto
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Draws the Lite map as PNG with the layers of the JavaScript map (frontend `util/layer-construction.ts`), in the
 * same order: country borders, highlighted countries, relay counts per location, relay markers, family and
 * autonomous system circles. Uses only Java2D shapes and no text, so no fonts are needed on the server.
 */
@Component
class LiteMapRenderer(
    private val worldBorders: WorldBorders,
    private val countryMeta: CountryMeta,
) {
    companion object {
        // Colors of the frontend: map background, offline borders and default relay type colors
        private val WATER = Color(0x2c353c)
        private val LAND = Color(0x3a444b)
        private val BORDER = Color(0x5b666e)
        val RELAY_TYPE_COLORS = mapOf(
            LiteRelayType.Exit to Color(0xff4848),
            LiteRelayType.Guard to Color(0xffd371),
            LiteRelayType.Other to Color(0xb78aff),
        )
        // Relay markers while families or autonomous systems are shown
        private val SINGLE_COLOR = Color(0x989898)
        // Leaflet's default color of paths, used for the outline of countries with relays
        private val LEAFLET_BLUE = Color(0x3388ff)

        // A Leaflet circle marker has a stroke of 3 pixels around its radius
        private const val MARKER_STROKE = 3f
        private const val DEFAULT_MARKER_RADIUS = 1.0
        private const val GROUP_FILL_ALPHA = .2f

        /** The radius of a marker relative to its consensus weight, like calcRadiusForWeight() */
        fun radiusForWeight(weight: Long, maxWeight: Long): Double {
            val minRadius = 2.0
            val maxRadius = 20.0
            if (maxWeight <= 0) return minRadius
            return minRadius + (maxRadius - minRadius) * sqrt(min(weight, maxWeight).toDouble() / maxWeight)
        }

        /** The radius of a circle for a number of relays, like calcRadiusForValue() */
        fun radiusForCount(value: Int): Double {
            fun mapRange(value: Int, x1: Int, y1: Int, x2: Int, y2: Int) = (value - x1) * (y2 - x2).toDouble() / (y1 - x1) + x2
            return when {
                value < 10 -> 10.0
                value < 100 -> mapRange(value, 10, 100, 10, 50)
                value < 200 -> mapRange(value, 100, 200, 50, 70)
                value < 300 -> mapRange(value, 200, 300, 70, 100)
                else -> 100.0
            }
        }

        fun familyColor(familyId: Long): Color = hsl((familyId % 8) * (360.0 / 8), .9, .6)

        fun autonomousSystemColor(asNumber: Long): Color = hsl((asNumber % 12) * (360.0 / 12), .75, .55)

        /** Converts a CSS hsl() color */
        fun hsl(hue: Double, saturation: Double, lightness: Double): Color {
            val chroma = (1 - kotlin.math.abs(2 * lightness - 1)) * saturation
            fun channel(n: Int): Float {
                val k = (n + hue / 30) % 12
                return (lightness - chroma / 2 * maxOf(-1.0, minOf(k - 3, 9 - k, 1.0))).toFloat()
            }
            return Color(channel(0), channel(8), channel(4))
        }
    }

    /**
     * Draws the relays of the view
     * @param relays - all relays of the view's day, they are filtered here
     */
    fun render(view: LiteView, relays: List<RelayLocationDto>): ByteArray {
        val filtered = LiteAggregation.filterRelays(relays, view)
        val image = BufferedImage(view.size.width, view.size.height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            graphics.color = WATER
            graphics.fillRect(0, 0, view.size.width, view.size.height)
            drawCountries(graphics, view, filtered)
            if (Grouping.Coordinates in view.groupings) drawRelayCounts(graphics, view, filtered)
            if (Grouping.Country in view.groupings) drawCountryMarkers(graphics, view, filtered) else drawRelayMarkers(graphics, view, filtered)
            if (Grouping.Family in view.groupings) {
                drawGroups(graphics, view, filtered, view.selectedFamily?.toLong(), ::familyColor) { it.familyId }
            }
            if (Grouping.AutonomousSystem in view.groupings) {
                drawGroups(graphics, view, filtered, view.selectedAutonomousSystem?.toLong(), ::autonomousSystemColor) { it.asNumber?.toLong() }
            }
        } finally {
            graphics.dispose()
        }
        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "png", output)
            output.toByteArray()
        }
    }

    /**
     * The land, and with the country grouping a highlight of each country that has relays, like buildCountryLayer()
     */
    private fun drawCountries(graphics: Graphics2D, view: LiteView, relays: List<RelayLocationDto>) {
        // World pixels of zoom level 0 to pixels of the image
        val scale = WebMercator.worldSize(view.zoom) / WebMercator.worldSize(0)
        val transform = AffineTransform.getTranslateInstance(-view.left, -view.top).apply { scale(scale, scale) }
        val visible = Rectangle2D.Double(0.0, 0.0, view.size.width.toDouble(), view.size.height.toDouble())
        val countriesWithRelays = if (Grouping.Country in view.groupings) relays.map { it.country }.toSet() else emptySet()
        for (country in worldBorders.countries) {
            if (!transform.createTransformedShape(country.bounds).intersects(visible)) continue
            val shape = transform.createTransformedShape(country.path)
            graphics.color = LAND
            graphics.fill(shape)
            graphics.stroke = BasicStroke(0.7f)
            graphics.color = BORDER
            graphics.draw(shape)
            if (country.isoCode in countriesWithRelays) {
                val alpha = if (country.isoCode == view.selectedCountry) .7f else .3f
                graphics.color = Color(1f, 1f, 1f, alpha)
                graphics.fill(shape)
                graphics.stroke = BasicStroke(0.5f)
                graphics.color = LEAFLET_BLUE
                graphics.draw(shape)
            }
        }
    }

    /**
     * One marker per location, colored by its most important relay type (exit before guard before other), or sized
     * by the consensus weight of its relays, like buildRelayLayer()
     */
    private fun drawRelayMarkers(graphics: Graphics2D, view: LiteView, relays: List<RelayLocationDto>) {
        val singleColor = Grouping.Family in view.groupings || Grouping.AutonomousSystem in view.groupings
        val locations = LiteAggregation.byLocation(relays)
        val weights = if (view.sizeByWeight) locations.mapValues { (_, relaysAtLocation) -> relaysAtLocation.sumOf { it.consensusWeight ?: 0 } } else emptyMap()
        // Without any weight, e.g. for days that were not reprocessed yet, all markers keep their default size
        val maxWeight = weights.values.maxOrNull() ?: 0
        locations.entries
            .map { (location, relaysAtLocation) -> Triple(location, relaysAtLocation.minOf { LiteRelayType.of(it.flags) }, weights[location]) }
            // Draw the more important types last, so they are on top
            .sortedByDescending { it.second }
            .forEach { (location, type, weight) ->
                val radius = if (maxWeight > 0 && weight != null) radiusForWeight(weight, maxWeight) else DEFAULT_MARKER_RADIUS
                val color = if (singleColor) SINGLE_COLOR else RELAY_TYPE_COLORS.getValue(type)
                drawCircle(graphics, view, location, radius, color, MARKER_STROKE, GROUP_FILL_ALPHA)
            }
    }

    /**
     * One marker per relay in the color of its country, gray outside the selected country, like buildRelayCountryLayer()
     */
    private fun drawCountryMarkers(graphics: Graphics2D, view: LiteView, relays: List<RelayLocationDto>) {
        relays.forEach { relay ->
            val selected = view.selectedCountry == null || view.selectedCountry == relay.country
            val color = hsl(countryMeta.mapColor(relay.country) * 360.0 / 9, if (selected) .9 else 0.0, .6)
            drawCircle(graphics, view, relay.lat.toDouble() to relay.long.toDouble(), DEFAULT_MARKER_RADIUS, color, MARKER_STROKE, GROUP_FILL_ALPHA)
        }
    }

    /**
     * A white circle for every location with at least 4 relays, sized by their number, like buildAggregatedCoordinatesLayer()
     */
    private fun drawRelayCounts(graphics: Graphics2D, view: LiteView, relays: List<RelayLocationDto>) {
        LiteAggregation.byLocation(relays).forEach { (location, relaysAtLocation) ->
            if (relaysAtLocation.size < 4) return@forEach
            drawCircle(graphics, view, location, radiusForCount(relaysAtLocation.size), Color.WHITE, .3f, GROUP_FILL_ALPHA)
        }
    }

    /**
     * Circles for the groups at each location, or only for the selected group, like buildGroupCoordinatesLayer() and
     * buildSelectedGroupLayer()
     */
    private fun drawGroups(
        graphics: Graphics2D,
        view: LiteView,
        relays: List<RelayLocationDto>,
        selectedId: Long?,
        color: (Long) -> Color,
        groupOf: (RelayLocationDto) -> Long?,
    ) {
        if (selectedId != null) {
            LiteAggregation.byLocation(relays.filter { groupOf(it) == selectedId }).forEach { (location, relaysAtLocation) ->
                drawCircle(graphics, view, location, radiusForCount(relaysAtLocation.size), color(selectedId), 1f, GROUP_FILL_ALPHA)
            }
            return
        }
        LiteAggregation.groupsByLocation(relays, groupOf).forEach { (location, groups) ->
            groups.forEach { group ->
                drawCircle(graphics, view, location, radiusForCount(group.relays.size + group.padding), color(group.groupId), 1f, GROUP_FILL_ALPHA)
            }
        }
    }

    /**
     * Draws a circle like a Leaflet circle marker: a translucent fill and a stroke centered on its outline
     */
    private fun drawCircle(
        graphics: Graphics2D,
        view: LiteView,
        location: Pair<Double, Double>,
        radius: Double,
        color: Color,
        strokeWidth: Float,
        fillAlpha: Float,
    ) {
        val x = WebMercator.x(location.second, view.zoom) - view.left
        val y = WebMercator.y(location.first, view.zoom) - view.top
        val reach = radius + strokeWidth
        if (x < -reach || y < -reach || x > view.size.width + reach || y > view.size.height + reach) return
        val circle = Ellipse2D.Double(x - radius, y - radius, 2 * radius, 2 * radius)
        graphics.color = Color(color.red, color.green, color.blue, (fillAlpha * 255).toInt())
        graphics.fill(circle)
        graphics.color = color
        graphics.stroke = BasicStroke(strokeWidth)
        graphics.draw(circle)
    }
}
