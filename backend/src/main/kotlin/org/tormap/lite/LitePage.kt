package org.tormap.lite

import org.tormap.database.entity.TorRelayFlag
import java.time.LocalDate

/**
 * The data shown on a Lite page besides the [LiteView] itself
 */
class LitePageModel(
    val view: LiteView,
    val firstDay: LocalDate?,
    val lastDay: LocalDate?,
    val hasPreviousDay: Boolean,
    val hasNextDay: Boolean,
    val shownRelayCount: Int,
    val totalRelayCount: Int,
    val notice: String?,
    val fullVersionUrl: String,
)

/**
 * Renders the Lite page: a single GET form, so every button, the map and the settings work without JavaScript.
 */
object LitePage {
    const val PATH = "/lite/"
    const val MAP_PATH = "/lite/map.png"
    private const val STYLESHEET_PATH = "/static/lite/lite.css"

    fun render(model: LitePageModel): String = Html.document {
        val view = model.view
        tag("html", "lang" to "en") {
            tag("head") {
                tag("meta", "charset" to "utf-8")
                tag("meta", "name" to "viewport", "content" to "width=device-width, initial-scale=1")
                tag("title", "TorMap Lite" + (view.day?.let { " - $it" } ?: ""))
                tag("link", "rel" to "stylesheet", "href" to STYLESHEET_PATH)
                tag("link", "rel" to "icon", "href" to "/favicon.ico")
            }
            tag("body") {
                tag("header") {
                    tag("h1") {
                        text("TorMap ")
                        tag("span", "Lite", "class" to "subtitle")
                    }
                    tag("p", "class" to "note") {
                        text("This version of TorMap works without JavaScript. With JavaScript enabled, ")
                        tag("a", "the full map", "href" to "${model.fullVersionUrl}/#${view.toPermalinkHash()}")
                        text(" has more features.")
                    }
                }
                tag("main") {
                    model.notice?.let { tag("p", it, "class" to "notice", "role" to "status") }
                    tag("form", "method" to "get", "action" to PATH) {
                        dayControls(model)
                        hiddenInput("lat", view.latitudeParameter)
                        hiddenInput("lng", view.longitudeParameter)
                        hiddenInput("zoom", view.zoom.toString())
                        mapControls(view)
                        map(model)
                        settings(view)
                    }
                }
                tag("footer") {
                    tag("p") {
                        text("Relay data from ")
                        tag("a", "Tor Project CollecTor", "href" to "https://collector.torproject.org/")
                        text(". Source code on ")
                        tag("a", "GitHub", "href" to "https://github.com/TorMap/tormap")
                        text(".")
                    }
                }
            }
        }
    }

    private fun Html.dayControls(model: LitePageModel) {
        tag("fieldset", "class" to "day") {
            tag("label", "Day", "for" to "day")
            tag(
                "input",
                "type" to "date",
                "id" to "day",
                "name" to "day",
                "value" to model.view.day,
                "min" to model.firstDay,
                "max" to model.lastDay,
            )
            // The first submit button of the form is the one used when pressing enter in the date field
            tag("button", "Show", "type" to "submit")
            tag("button", "‹ Previous day", "type" to "submit", "name" to "go", "value" to "previous", "disabled" to !model.hasPreviousDay)
            tag("button", "Next day ›", "type" to "submit", "name" to "go", "value" to "next", "disabled" to !model.hasNextDay)
            tag("button", "Latest day", "type" to "submit", "name" to "go", "value" to "latest", "disabled" to !model.hasNextDay)
        }
    }

    private fun Html.mapControls(view: LiteView) {
        tag("div", "class" to "map-controls", "role" to "group", "aria-label" to "Map controls") {
            mapButton("in", "+", "Zoom in", !view.canZoomIn)
            mapButton("out", "−", "Zoom out", !view.canZoomOut)
            mapButton("west", "←", "Move west")
            mapButton("north", "↑", "Move north")
            mapButton("south", "↓", "Move south")
            mapButton("east", "→", "Move east")
        }
    }

    private fun Html.mapButton(action: String, symbol: String, label: String, disabled: Boolean = false) =
        tag("button", symbol, "type" to "submit", "name" to "go", "value" to action, "title" to label, "aria-label" to label, "disabled" to disabled)

    private fun Html.map(model: LitePageModel) {
        val view = model.view
        val dayText = view.day?.toString() ?: "no day"
        tag("div", "class" to "map") {
            // A click submits the form with map.x and map.y, the pixel that was clicked
            tag(
                "input",
                "type" to "image",
                "name" to "map",
                "src" to "$MAP_PATH?${view.toQuery()}",
                "width" to view.size.width,
                "height" to view.size.height,
                "alt" to "Map of ${model.shownRelayCount} relays on $dayText. Click to zoom in.",
            )
        }
        tag("p", "class" to "summary") {
            text("Showing ${model.shownRelayCount} of ${model.totalRelayCount} relays on $dayText, zoom level ${view.zoom}. Click the map to zoom in.")
        }
    }

    private fun Html.settings(view: LiteView) {
        tag("div", "class" to "settings") {
            hiddenInput("f", "1")
            tag("fieldset") {
                tag("legend", "Relay types")
                LiteRelayType.entries.forEach { type ->
                    checkbox("type", type.parameter, type in view.relayTypes) {
                        tag("span", "class" to "swatch swatch-${type.parameter}", "aria-hidden" to "true")
                        text(type.label)
                    }
                }
            }
            tag("fieldset") {
                tag("legend", "Relays must have the flags")
                TorRelayFlag.entries.forEach { flag ->
                    checkbox("flag", flag.name, flag in view.requiredFlags) { text(flag.name) }
                }
            }
            tag("fieldset") {
                tag("legend", "Map size")
                MapSize.entries.forEach { size ->
                    tag("label") {
                        tag("input", "type" to "radio", "name" to "size", "value" to size.parameter, "checked" to (size == view.size))
                        text(" ${size.label} (${size.width} × ${size.height})")
                    }
                }
            }
            tag("button", "Apply", "type" to "submit")
        }
    }

    private fun Html.checkbox(name: String, value: String, checked: Boolean, label: Html.() -> Unit) {
        tag("label") {
            tag("input", "type" to "checkbox", "name" to name, "value" to value, "checked" to checked)
            text(" ")
            label()
        }
    }

    private fun Html.hiddenInput(name: String, value: String) =
        tag("input", "type" to "hidden", "name" to name, "value" to value)
}
