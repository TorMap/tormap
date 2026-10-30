package org.tormap.lite

import org.tormap.adapter.dto.RelayLocationDto
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The heatmap of the JavaScript map (frontend `util/heat-layer.ts`, a port of Leaflet.heat and simpleheat) with the
 * options of buildRelayHeatmapLayer(). Points in the same grid cell are merged, each point draws a blurred circle
 * into an alpha channel and the alpha is mapped to a color gradient.
 */
object LiteHeatmap {
    private const val RADIUS = 25
    private const val BLUR = 35
    private const val MAX = 1.0
    private const val MIN_OPACITY = .55
    // The maximum zoom of the JavaScript map, points reach their full intensity there
    private const val MAP_MAX_ZOOM = 18
    private const val OUTER_RADIUS = RADIUS + BLUR
    private const val CELL_SIZE = OUTER_RADIUS / 2.0
    private val GRADIENT_STOPS = listOf(.4 to Color(0x2e53dc), .65 to Color(0xc924ae), .75 to Color(0xff4646), .83 to Color(0xff0000))

    /**
     * The alpha of the blurred circle: a canvas shadow blur of [BLUR] pixels is a Gaussian blur with a standard
     * deviation of half the blur, which turns the edge of the circle into a smooth falloff
     */
    private val stamp: Array<FloatArray> = run {
        val sigma = BLUR / 2.0
        Array(2 * OUTER_RADIUS) { y ->
            FloatArray(2 * OUTER_RADIUS) { x ->
                val distance = sqrt((x + .5 - OUTER_RADIUS).pow(2) + (y + .5 - OUTER_RADIUS).pow(2))
                (.5 * erfc((distance - RADIUS) / (sigma * sqrt(2.0)))).toFloat()
            }
        }
    }

    /** 256 colors for the alpha values, like the 1 pixel wide gradient canvas of simpleheat */
    private val palette: IntArray = IntArray(256) { index ->
        val position = index / 255.0
        val upper = GRADIENT_STOPS.indexOfFirst { it.first >= position }
        when (upper) {
            -1 -> GRADIENT_STOPS.last().second.rgb
            0 -> GRADIENT_STOPS.first().second.rgb
            else -> {
                val (lowStop, low) = GRADIENT_STOPS[upper - 1]
                val (highStop, high) = GRADIENT_STOPS[upper]
                val t = (position - lowStop) / (highStop - lowStop)
                fun mix(a: Int, b: Int) = (a + (b - a) * t).roundToInt()
                Color(mix(low.red, high.red), mix(low.green, high.green), mix(low.blue, high.blue)).rgb
            }
        }
    }

    /**
     * Returns a transparent image of the view's size with the heatmap of the relays
     */
    fun render(view: LiteView, relays: List<RelayLocationDto>): BufferedImage {
        val width = view.size.width
        val height = view.size.height
        val intensity = 1 / 2.0.pow((MAP_MAX_ZOOM - view.zoom).coerceIn(0, 12))

        // Merge points that fall into the same grid cell, weighted by intensity. The grid is fixed to the world, so
        // moving the map does not change which points are merged.
        val cells = HashMap<Long, DoubleArray>()
        for (relay in relays) {
            val worldX = WebMercator.x(relay.long.toDouble(), view.zoom)
            val worldY = WebMercator.y(relay.lat.toDouble(), view.zoom)
            val x = worldX - view.left
            val y = worldY - view.top
            if (x < -OUTER_RADIUS || y < -OUTER_RADIUS || x > width + OUTER_RADIUS || y > height + OUTER_RADIUS) continue
            val key = floor(worldX / CELL_SIZE).toLong() * 1_000_000 + floor(worldY / CELL_SIZE).toLong()
            val cell = cells[key]
            if (cell == null) {
                cells[key] = doubleArrayOf(x, y, intensity)
            } else {
                cell[0] = (cell[0] * cell[2] + x * intensity) / (cell[2] + intensity)
                cell[1] = (cell[1] * cell[2] + y * intensity) / (cell[2] + intensity)
                cell[2] += intensity
            }
        }

        val alpha = FloatArray(width * height)
        for ((x, y, value) in cells.values.map { Triple(it[0].roundToInt(), it[1].roundToInt(), min(it[2], MAX)) }) {
            val opacity = max(value / MAX, MIN_OPACITY).toFloat()
            for (stampY in 0 until 2 * OUTER_RADIUS) {
                val imageY = y - OUTER_RADIUS + stampY
                if (imageY !in 0 until height) continue
                val row = stamp[stampY]
                for (stampX in 0 until 2 * OUTER_RADIUS) {
                    val imageX = x - OUTER_RADIUS + stampX
                    if (imageX !in 0 until width) continue
                    // Drawing with source-over, like the canvas does
                    val source = opacity * row[stampX]
                    val index = imageY * width + imageX
                    alpha[index] = source + alpha[index] * (1 - source)
                }
            }
        }

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (index in alpha.indices) {
            val value = (alpha[index] * 255).roundToInt().coerceIn(0, 255)
            if (value == 0) continue
            image.setRGB(index % width, index / width, (value shl 24) or (palette[value] and 0xffffff))
        }
        return image
    }

    /** The complementary error function (Abramowitz and Stegun 7.1.26, accurate to about 1e-7) */
    private fun erfc(x: Double): Double {
        val t = 1 / (1 + .3275911 * abs(x))
        val polynomial = t * (.254829592 + t * (-.284496736 + t * (1.421413741 + t * (-1.453152027 + t * 1.061405429))))
        val erfcOfAbs = polynomial * exp(-x * x)
        return if (x >= 0) erfcOfAbs else 2 - erfcOfAbs
    }
}
