package org.tormap.lite

import org.tormap.adapter.dto.RelayLocationDto
import org.tormap.database.entity.TorRelayFlag
import kotlin.math.hypot

/**
 * Finding relays of a day for the Lite pages
 */
object LiteRelays {
    /** How far from a click relays are listed, a little more than a marker so small screens can hit it */
    const val PICK_RADIUS_PIXELS = 8.0

    /** Longer lists are cut, e.g. a click on a whole continent at a low zoom level */
    const val MAX_LISTED_RELAYS = 500

    const val MAX_QUERY_LENGTH = 100

    private val fingerprintPattern = Regex("^[0-9A-Fa-f]{40}$")

    /**
     * Returns the relays whose marker is drawn within [PICK_RADIUS_PIXELS] of a pixel of the map image, closest first
     */
    fun near(relays: List<RelayLocationDto>, view: LiteView, imageX: Int, imageY: Int): List<RelayLocationDto> = relays
        .map { relay ->
            val x = WebMercator.x(relay.long.toDouble(), view.zoom) - view.left
            val y = WebMercator.y(relay.lat.toDouble(), view.zoom) - view.top
            relay to hypot(x - imageX, y - imageY)
        }
        .filter { (_, distance) -> distance <= PICK_RADIUS_PIXELS }
        .sortedWith(compareBy({ it.second }, { it.first.nickname?.lowercase() }))
        .map { it.first }

    /**
     * Whether a search query is a relay fingerprint, which is looked up in the relay details
     */
    fun isFingerprint(query: String) = fingerprintPattern.matches(query)

    /**
     * Returns the relays whose nickname contains the query (ignoring case) or whose details ID matches
     */
    fun search(relays: List<RelayLocationDto>, query: String, detailsIds: Set<Long>): List<RelayLocationDto> {
        val lowercaseQuery = query.lowercase()
        return relays
            .filter { relay ->
                relay.detailsId in detailsIds || relay.nickname?.lowercase()?.contains(lowercaseQuery) == true
            }
            .sortedBy { it.nickname?.lowercase() }
    }

    /**
     * The names of the flags of a relay, in the order of [TorRelayFlag]
     */
    fun flagNames(flags: List<Int>?): List<String> =
        TorRelayFlag.entries.filter { flags?.contains(it.ordinal) == true }.map { it.name }
}
