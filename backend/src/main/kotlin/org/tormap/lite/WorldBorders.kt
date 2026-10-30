package org.tormap.lite

import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D

/**
 * The country borders of the frontend's world.geo.json (copied into the resources by the build), projected once
 * at startup. Paths are in Web Mercator pixels of zoom level 0, so they only need to be scaled for other zoom levels.
 */
@Component
class WorldBorders(objectMapper: ObjectMapper) {
    class Country(val isoCode: String?, val path: Path2D.Float, val bounds: Rectangle2D)

    val countries: List<Country> = WorldBorders::class.java.getResourceAsStream(RESOURCE)
        .let { requireNotNull(it) { "Missing resource $RESOURCE" } }
        .use { objectMapper.readTree(it) }
        .get("features")
        .mapNotNull { feature ->
            val path = feature.get("geometry")?.let(::toPath) ?: return@mapNotNull null
            Country(feature.get("properties")?.get("iso_a2")?.asString(), path, path.bounds2D)
        }

    private fun toPath(geometry: JsonNode): Path2D.Float? {
        val polygons = when (geometry.get("type")?.asString()) {
            "Polygon" -> listOf(geometry.get("coordinates"))
            "MultiPolygon" -> geometry.get("coordinates").toList()
            else -> return null
        }
        val path = Path2D.Float(Path2D.WIND_EVEN_ODD)
        for (polygon in polygons) {
            for (ring in polygon) {
                ring.forEachIndexed { index, point ->
                    val x = WebMercator.x(point.get(0).asDouble(), 0)
                    val y = WebMercator.y(point.get(1).asDouble(), 0)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.closePath()
            }
        }
        return path
    }

    companion object {
        private const val RESOURCE = "/lite/world.geo.json"
    }
}
