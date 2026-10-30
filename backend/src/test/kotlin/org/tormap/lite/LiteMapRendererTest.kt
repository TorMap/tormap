package org.tormap.lite

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.tormap.adapter.dto.RelayLocationDto
import org.tormap.database.entity.TorRelayFlag
import tools.jackson.databind.json.JsonMapper
import java.awt.Color
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import javax.imageio.ImageIO

class LiteMapRendererTest : StringSpec({
    val renderer = LiteMapRenderer(WorldBorders(JsonMapper.builder().build()))

    fun relay(latitude: Double, longitude: Double, vararg flags: TorRelayFlag) = RelayLocationDto(
        BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude), "DE",
        flags.joinToString(", ") { it.ordinal.toString() }, 1, null, "relay", 100, null,
    )

    fun pixel(png: ByteArray, view: LiteView, latitude: Double, longitude: Double): Color {
        val image = ImageIO.read(ByteArrayInputStream(png))
        image.width shouldBe view.size.width
        image.height shouldBe view.size.height
        val x = (WebMercator.x(longitude, view.zoom) - view.left).toInt()
        val y = (WebMercator.y(latitude, view.zoom) - view.top).toInt()
        return Color(image.getRGB(x, y))
    }

    val berlin = 52.52 to 13.40
    val view = LiteView.parse(mapOf("lat" to listOf("52.52"), "lng" to listOf("13.40"), "zoom" to listOf("6")))

    "draws a relay marker in the color of the most important type at its location" {
        val relays = listOf(
            relay(berlin.first, berlin.second, TorRelayFlag.Guard),
            relay(berlin.first, berlin.second, TorRelayFlag.Exit, TorRelayFlag.Guard),
        )
        pixel(renderer.render(view, relays), view, berlin.first, berlin.second) shouldBe Color(0xff4848)
    }

    "draws land without relays" {
        val color = pixel(renderer.render(view, emptyList()), view, berlin.first, berlin.second)
        color shouldNotBe Color(0xff4848)
        color shouldNotBe Color(0x2c353c)
    }

    "filters relays by type and required flags" {
        val exit = relay(0.0, 0.0, TorRelayFlag.Exit, TorRelayFlag.Stable)
        val guard = relay(0.0, 0.0, TorRelayFlag.Guard)
        val other = relay(0.0, 0.0)
        val relays = listOf(exit, guard, other)
        renderer.filterRelays(relays, view) shouldBe relays
        renderer.filterRelays(relays, view.copy(relayTypes = setOf(LiteRelayType.Guard, LiteRelayType.Other))) shouldBe listOf(guard, other)
        renderer.filterRelays(relays, view.copy(requiredFlags = setOf(TorRelayFlag.Stable))) shouldBe listOf(exit)
    }
})
