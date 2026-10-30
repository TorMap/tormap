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
    val mapper = JsonMapper.builder().build()
    val renderer = LiteMapRenderer(WorldBorders(mapper), CountryMeta(mapper))

    fun relay(latitude: Double, longitude: Double, vararg flags: TorRelayFlag, familyId: Long? = null) = RelayLocationDto(
        BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude), "DE",
        flags.joinToString(", ") { it.ordinal.toString() }, 1, familyId, "relay", 100, null,
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
        // Antialiasing blends the marker with the land, so only check that it is red (exit) and not yellow (guard)
        val color = pixel(renderer.render(view, relays), view, berlin.first, berlin.second)
        (color.red - color.green > 100) shouldBe true
    }

    "draws land without relays" {
        val color = pixel(renderer.render(view, emptyList()), view, berlin.first, berlin.second)
        color shouldNotBe Color(0xff4848)
        color shouldNotBe Color(0x2c353c)
    }

    "draws relays in a single color while families are shown, with a circle in the family color" {
        val relays = (1..5).map { relay(berlin.first, berlin.second, TorRelayFlag.Exit, familyId = 3) }
        val familyView = view.showGroup(Grouping.Family)
        pixel(renderer.render(familyView, relays), familyView, berlin.first, berlin.second) shouldNotBe Color(0xff4848)
    }

    "draws every grouping without failing" {
        val relays = (1..5).map { relay(berlin.first, berlin.second, TorRelayFlag.Guard, familyId = it.toLong()) }
        val allGroups = Grouping.entries.fold(view.copy(sizeByWeight = true, selectedCountry = "DE")) { it, grouping -> it.showGroup(grouping) }
        ImageIO.read(ByteArrayInputStream(renderer.render(allGroups, relays))).width shouldBe view.size.width
    }

    "converts CSS hsl colors" {
        LiteMapRenderer.hsl(0.0, 1.0, .5) shouldBe Color.RED
        LiteMapRenderer.hsl(120.0, 1.0, .5) shouldBe Color.GREEN
        LiteMapRenderer.hsl(0.0, 0.0, 1.0) shouldBe Color.WHITE
    }

    "sizes circles like the frontend" {
        LiteMapRenderer.radiusForCount(4) shouldBe 10.0
        LiteMapRenderer.radiusForCount(55) shouldBe 30.0
        LiteMapRenderer.radiusForCount(1000) shouldBe 100.0
        LiteMapRenderer.radiusForWeight(0, 0) shouldBe 2.0
        LiteMapRenderer.radiusForWeight(100, 100) shouldBe 20.0
    }
})
