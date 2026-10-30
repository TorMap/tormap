package org.tormap.lite

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.tormap.adapter.dto.RelayLocationDto
import org.tormap.database.entity.TorRelayFlag
import java.math.BigDecimal

class LiteRelaysTest : StringSpec({
    fun relay(nickname: String, latitude: Double, longitude: Double, detailsId: Long = 1) = RelayLocationDto(
        BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude), "DE", "${TorRelayFlag.Running.ordinal}, ${TorRelayFlag.Exit.ordinal}",
        detailsId, null, nickname, null, null,
    )

    val view = LiteView.parse(mapOf("lat" to listOf("50"), "lng" to listOf("10"), "zoom" to listOf("8")))
    fun pixelOf(relay: RelayLocationDto) = Pair(
        (WebMercator.x(relay.long.toDouble(), view.zoom) - view.left).toInt(),
        (WebMercator.y(relay.lat.toDouble(), view.zoom) - view.top).toInt(),
    )

    "lists the relays near a click, closest first" {
        val close = relay("close", 50.0, 10.0)
        val closer = relay("closer", 50.0, 10.01)
        val far = relay("far", 52.0, 13.0)
        val (x, y) = pixelOf(closer)
        LiteRelays.near(listOf(far, close, closer), view, x, y) shouldBe listOf(closer, close)
    }

    "searches nicknames ignoring case and matches fingerprints by details ID" {
        val alpha = relay("AlphaRelay", 0.0, 0.0, 1)
        val beta = relay("beta", 0.0, 0.0, 2)
        LiteRelays.search(listOf(beta, alpha), "alpha", emptySet()) shouldBe listOf(alpha)
        LiteRelays.search(listOf(beta, alpha), "A".repeat(40), setOf(2L)) shouldBe listOf(beta)
    }

    "recognizes fingerprints" {
        LiteRelays.isFingerprint("0123456789abcdefABCDEF0123456789ABCDEF01") shouldBe true
        LiteRelays.isFingerprint("relay") shouldBe false
    }

    "names flags in their order" {
        LiteRelays.flagNames(listOf(TorRelayFlag.Exit.ordinal, TorRelayFlag.Valid.ordinal)) shouldBe listOf("Valid", "Exit")
        LiteRelays.flagNames(null) shouldBe emptyList()
    }
})
