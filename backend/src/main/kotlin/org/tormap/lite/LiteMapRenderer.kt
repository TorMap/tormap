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

/**
 * Draws the Lite map as PNG: country borders like the offline map of the frontend and one marker per relay location,
 * colored by the most important relay type at that location (exit before guard before other), like the JavaScript map.
 * Uses only Java2D shapes and no text, so no fonts are needed on the server.
 */
@Component
class LiteMapRenderer(private val worldBorders: WorldBorders) {
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

        // A Leaflet circle marker with radius 1 and a stroke of 3 pixels
        private const val MARKER_RADIUS = 2.5
    }

    /**
     * Returns the relays that match the relay types and required flags of the view, like filterRelaysByFlags()
     */
    fun filterRelays(relays: List<RelayLocationDto>, view: LiteView): List<RelayLocationDto> {
        val requiredFlags = view.requiredFlags.map { it.ordinal }
        return relays.filter { relay ->
            LiteRelayType.of(relay.flags) in view.relayTypes && relay.flags.orEmpty().containsAll(requiredFlags)
        }
    }

    fun render(view: LiteView, relays: List<RelayLocationDto>): ByteArray {
        val width = view.size.width
        val height = view.size.height
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            graphics.color = WATER
            graphics.fillRect(0, 0, width, height)

            // World pixels of zoom level 0 to pixels of the image
            val transform = AffineTransform.getTranslateInstance(-view.left, -view.top).apply {
                scale(WebMercator.worldSize(view.zoom) / WebMercator.worldSize(0), WebMercator.worldSize(view.zoom) / WebMercator.worldSize(0))
            }
            val visible = Rectangle2D.Double(0.0, 0.0, width.toDouble(), height.toDouble())
            graphics.stroke = BasicStroke(0.7f)
            for (country in worldBorders.countries) {
                if (!transform.createTransformedShape(country.bounds).intersects(visible)) continue
                val shape = transform.createTransformedShape(country.path)
                graphics.color = LAND
                graphics.fill(shape)
                graphics.color = BORDER
                graphics.draw(shape)
            }

            drawMarkers(graphics, view, filterRelays(relays, view))
        } finally {
            graphics.dispose()
        }
        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "png", output)
            output.toByteArray()
        }
    }

    private fun drawMarkers(graphics: Graphics2D, view: LiteView, relays: List<RelayLocationDto>) {
        val typesByLocation = relays
            .groupBy { it.lat to it.long }
            .mapValues { (_, relaysAtLocation) -> relaysAtLocation.minOf { LiteRelayType.of(it.flags) } }
        // Draw the more important types last, so they are on top
        val marker = Ellipse2D.Double()
        typesByLocation.entries.sortedByDescending { it.value }.forEach { (location, type) ->
            val x = WebMercator.x(location.second.toDouble(), view.zoom) - view.left
            val y = WebMercator.y(location.first.toDouble(), view.zoom) - view.top
            if (x < -MARKER_RADIUS || y < -MARKER_RADIUS || x > view.size.width + MARKER_RADIUS || y > view.size.height + MARKER_RADIUS) return@forEach
            marker.setFrame(x - MARKER_RADIUS, y - MARKER_RADIUS, 2 * MARKER_RADIUS, 2 * MARKER_RADIUS)
            graphics.color = RELAY_TYPE_COLORS.getValue(type)
            graphics.fill(marker)
        }
    }
}
