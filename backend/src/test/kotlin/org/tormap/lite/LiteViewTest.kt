package org.tormap.lite

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.tormap.database.entity.TorRelayFlag
import java.time.LocalDate

class LiteViewTest : StringSpec({
    fun parse(query: String): LiteView = LiteView.parse(
        query.split("&").filter { it.isNotEmpty() }.map { it.substringBefore("=") to it.substringAfter("=") }
            .groupBy({ it.first }, { it.second })
    )

    "defaults show the whole world with all relay types" {
        val view = parse("")
        view.day shouldBe null
        view.zoom shouldBe 2
        view.size shouldBe MapSize.Large
        view.relayTypes shouldBe LiteRelayType.entries.toSet()
        view.requiredFlags shouldBe emptySet()
        view.longitude shouldBe (0.0 plusOrMinus 0.0001)
    }

    "invalid values fall back to defaults instead of failing" {
        val view = parse("day=2024-02-30&lat=abc&lng=500&zoom=99&size=huge&flag=Evil&type=exit")
        view shouldBe parse("")
    }

    "latitude and longitude are only used as a pair" {
        parse("lat=50&zoom=5") shouldBe parse("zoom=5")
    }

    "the canonical query parses to the same view" {
        val view = parse("day=2024-05-01&lat=52.52&lng=13.4&zoom=7&size=small&f=1&type=exit&type=guard&flag=Stable&flag=Fast")
        view.day shouldBe LocalDate.of(2024, 5, 1)
        view.relayTypes shouldBe setOf(LiteRelayType.Exit, LiteRelayType.Guard)
        view.requiredFlags shouldBe setOf(TorRelayFlag.Stable, TorRelayFlag.Fast)
        view.latitude shouldBe (52.52 plusOrMinus 0.1)
        parse(view.toQuery()) shouldBe view
    }

    "the click mode is part of the page query but not of the image query" {
        val view = parse("zoom=4&click=zoom")
        view.clickMode shouldBe ClickMode.Zoom
        view.toQuery() shouldContain "click=zoom"
        view.toImageQuery() shouldNotContain "click"
        parse("click=nonsense").clickMode shouldBe ClickMode.List
    }

    "unchecking all relay types is kept" {
        parse("f=1").relayTypes shouldBe emptySet()
    }

    "zooming keeps the center and stays within the zoom levels" {
        val view = parse("lat=48.85&lng=2.35&zoom=6")
        val zoomedIn = view.zoomedBy(1)
        zoomedIn.zoom shouldBe 7
        zoomedIn.latitude shouldBe (view.latitude plusOrMinus 0.1)
        zoomedIn.longitude shouldBe (view.longitude plusOrMinus 0.1)
        parse("zoom=${LiteView.MAX_ZOOM}").zoomedBy(1).zoom shouldBe LiteView.MAX_ZOOM
        parse("").zoomedBy(-1).zoom shouldBe LiteView.MIN_ZOOM
    }

    "a click zooms in on the clicked point" {
        val view = parse("lat=0&lng=0&zoom=4")
        val clicked = view.zoomedInAt(view.size.width / 2 + 256, view.size.height / 2)
        clicked.zoom shouldBe 5
        clicked.longitude shouldBe (WebMercator.longitude(view.centerX + 256, 4) plusOrMinus 0.5)
    }

    "panning stops at the edge of the world" {
        var view = parse("zoom=3")
        repeat(20) { view = view.panned(0.0, -0.5) }
        view.top shouldBe 0.0
        repeat(40) { view = view.panned(0.5, 0.0) }
        view.left + view.size.width shouldBe WebMercator.worldSize(3)
    }

    "the permalink of the JavaScript map has the same position and day" {
        parse("day=2024-05-01&lat=0&lng=0&zoom=4").toPermalinkHash() shouldBe "lat=0.0000&lng=0.0000&zoom=4&day=2024-05-01"
    }
})
