package org.tormap.lite

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sinh

/**
 * The Web Mercator projection used by Leaflet (EPSG:3857), so zoom levels and positions match the JavaScript map.
 * Pixel coordinates are relative to the top left corner of the world at the given zoom level.
 */
object WebMercator {
    private const val TILE_SIZE = 256.0
    const val MAX_LATITUDE = 85.0511287798

    fun worldSize(zoom: Int): Double = TILE_SIZE * (1 shl zoom)

    fun x(longitude: Double, zoom: Int): Double = (longitude + 180) / 360 * worldSize(zoom)

    fun y(latitude: Double, zoom: Int): Double {
        val sinLatitude = sin(Math.toRadians(latitude.coerceIn(-MAX_LATITUDE, MAX_LATITUDE)))
        return (0.5 - ln((1 + sinLatitude) / (1 - sinLatitude)) / (4 * PI)) * worldSize(zoom)
    }

    fun longitude(x: Double, zoom: Int): Double = x / worldSize(zoom) * 360 - 180

    fun latitude(y: Double, zoom: Int): Double = Math.toDegrees(atan(sinh(PI * (1 - 2 * y / worldSize(zoom)))))
}
