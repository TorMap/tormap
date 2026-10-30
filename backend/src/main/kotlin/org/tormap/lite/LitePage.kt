package org.tormap.lite

import org.tormap.adapter.dto.CountryUsageDto
import org.tormap.adapter.dto.RelayLocationDto
import org.tormap.database.entity.TorRelayFlag
import java.time.LocalDate
import java.util.Locale

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
    /** The relays around the clicked point of the map, null if nothing was clicked */
    val pickedRelays: List<RelayLocationDto>? = null,
    val statistics: LiteStatistics? = null,
    val rankings: List<Ranking> = emptyList(),
    /** The relays per day, null if there are not enough days */
    val timeline: LiteTimeline? = null,
    /** The estimated Tor users per country of the day, null if the view does not show them */
    val countryUsage: List<CountryUsageDto>? = null,
    /** The name of a country code */
    val countryName: (String) -> String = { it },
)

/** The largest groups of a grouping, e.g. families */
class Ranking(val title: String, val rows: List<RankingRow>)

/**
 * A group with its number of relays, the view that selects it on the map and optionally a page about it
 */
class RankingRow(val label: String, val relayCount: Int, val selectView: LiteView, val detailsPath: String?)

/**
 * Renders the Lite page: a single GET form, so every button, the map and the settings work without JavaScript.
 */
object LitePage {
    const val PATH = "/lite/"
    const val MAP_PATH = "/lite/map.png"
    const val TIMELINE_PATH = "/lite/timeline.png"
    const val RELAY_PATH = "/lite/relay/"
    const val FAMILY_PATH = "/lite/family/"
    const val AUTONOMOUS_SYSTEM_PATH = "/lite/as/"
    const val SEARCH_PATH = "/lite/search"
    private const val STYLESHEET_PATH = "/static/lite/lite.css"
    private const val USAGE_RANKING_SIZE = 10
    // Like the frontend, estimates based on fewer relays get a note
    private const val LOW_ESTIMATE_FRACTION = 30

    fun render(model: LitePageModel): String {
        val view = model.view
        return layout("TorMap Lite" + (view.day?.let { " - $it" } ?: ""), view, model.fullVersionUrl) {
            model.notice?.let { tag("p", it, "class" to "notice", "role" to "status") }
            tag("form", "method" to "get", "action" to PATH) {
                dayControls(model)
                model.timeline?.takeIf { it.isShown }?.let { timeline(it, view) }
                hiddenInput("lat", view.latitudeParameter)
                hiddenInput("lng", view.longitudeParameter)
                hiddenInput("zoom", view.zoom.toString())
                // Selections are made with links below the map and kept when the form is submitted
                view.selectedFamily?.let { hiddenInput("family", it.toString()) }
                view.selectedAutonomousSystem?.let { hiddenInput("as", it.toString()) }
                view.selectedCountry?.let { hiddenInput("country", it) }
                mapControls(view)
                map(model)
                selection(view)
                model.statistics?.let { statistics(it) }
                settings(view)
            }
            model.countryUsage?.let { countryUsage(it, model.countryName) }
            model.rankings.forEach { ranking(it, view) }
            model.pickedRelays?.let { relays ->
                tag("section", "id" to "relays") {
                    tag("h2", "Relays at the clicked point")
                    if (relays.isEmpty()) {
                        tag("p", "There are no relays shown there. Click closer to a marker or zoom in.")
                    } else {
                        relayTable(relays, view)
                    }
                }
            }
            searchForm(view, "")
        }
    }

    /**
     * The frame of every Lite page: header with the note about the JavaScript map, the content and the footer
     */
    fun layout(title: String, view: LiteView, fullVersionUrl: String, content: Html.() -> Unit): String = Html.document {
        tag("html", "lang" to "en") {
            tag("head") {
                tag("meta", "charset" to "utf-8")
                tag("meta", "name" to "viewport", "content" to "width=device-width, initial-scale=1")
                tag("title", title)
                tag("link", "rel" to "stylesheet", "href" to STYLESHEET_PATH)
                tag("link", "rel" to "icon", "href" to "/favicon.ico")
            }
            tag("body") {
                tag("header") {
                    tag("h1") {
                        tag("a", "href" to "$PATH?${view.toQuery()}", "class" to "home") { text("TorMap ") }
                        tag("span", "Lite", "class" to "subtitle")
                    }
                    tag("p", "class" to "note") {
                        text("This version of TorMap works without JavaScript. With JavaScript enabled, ")
                        tag("a", "the full map", "href" to "$fullVersionUrl/#${view.toPermalinkHash()}")
                        text(" has more features.")
                    }
                }
                tag("main") { content() }
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

    /**
     * A table of relays of the view's day with links to their details, family and autonomous system
     */
    fun Html.relayTable(relays: List<RelayLocationDto>, view: LiteView) {
        val query = view.toQuery()
        val listed = relays.take(LiteRelays.MAX_LISTED_RELAYS)
        tag("p", "class" to "summary") {
            text(if (listed.size < relays.size) "The first ${listed.size} of ${relays.size} relays:" else "${relays.size} relays:")
        }
        tag("div", "class" to "table") {
            tag("table") {
                tag("thead") {
                    tag("tr") {
                        listOf("Nickname", "Type", "Country", "Family", "Autonomous system", "Flags").forEach {
                            tag("th", it, "scope" to "col")
                        }
                    }
                }
                tag("tbody") {
                    listed.forEach { relay ->
                        val type = LiteRelayType.of(relay.flags)
                        tag("tr") {
                            tag("td") {
                                val nickname = relay.nickname ?: "Unknown"
                                if (relay.detailsId != null) tag("a", nickname, "href" to "$RELAY_PATH${relay.detailsId}?$query")
                                else text(nickname)
                            }
                            tag("td") {
                                tag("span", "class" to "swatch swatch-${type.parameter}", "aria-hidden" to "true")
                                text(type.label)
                            }
                            tag("td", relay.country)
                            tag("td") {
                                relay.familyId?.let { tag("a", "Family", "href" to "$FAMILY_PATH$it?$query") }
                            }
                            tag("td") {
                                relay.asNumber?.let { tag("a", "AS$it", "href" to "$AUTONOMOUS_SYSTEM_PATH$it?$query") }
                            }
                            tag("td", LiteRelays.flagNames(relay.flags).joinToString(", "), "class" to "flags")
                        }
                    }
                }
            }
        }
    }

    /**
     * A form to search the relays of the view's day. It is separate from the map form, because forms can not be nested.
     */
    fun Html.searchForm(view: LiteView, query: String) {
        tag("form", "method" to "get", "action" to SEARCH_PATH, "class" to "search", "role" to "search") {
            view.queryParameters().forEach { (name, value) -> hiddenInput(name, value) }
            tag("label", "Search relays of ${view.day ?: "the day"}", "for" to "q")
            tag(
                "input",
                "type" to "search",
                "id" to "q",
                "name" to "q",
                "value" to query,
                "maxlength" to LiteRelays.MAX_QUERY_LENGTH,
                "placeholder" to "Nickname or fingerprint",
                "required" to true,
            )
            tag("button", "Search", "type" to "submit")
        }
    }

    fun Html.backToMap(view: LiteView) = tag("p") { tag("a", "\u2190 Back to the map", "href" to "$PATH?${view.toQuery()}") }

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

    /**
     * The relays per day as an image input: a click submits timeline.x, which selects the day at that position
     */
    private fun Html.timeline(timeline: LiteTimeline, view: LiteView) {
        tag("div", "class" to "timeline") {
            tag(
                "input",
                "type" to "image",
                "name" to "timeline",
                "src" to "$TIMELINE_PATH?${timelineQuery(view)}",
                "width" to view.size.width,
                "height" to LiteTimeline.HEIGHT,
                "alt" to "Relays per day from ${timeline.days.first()} to ${timeline.days.last()}, between " +
                    "${timeline.minCount} and ${timeline.maxCount} relays. Click a position to show that day.",
            )
        }
        if (timeline.outliers.isEmpty()) return
        tag("details", "class" to "outliers") {
            tag("summary", "${timeline.outliers.size} days with an unusual number of relays")
            tag("ul") {
                timeline.outliers.asReversed().forEach { outlier ->
                    tag("li") {
                        tag("a", outlier.day.toString(), "href" to "$PATH?${view.withDay(outlier.day).toQuery()}")
                        val change = if (outlier.changePercent > 0) "+${outlier.changePercent}" else outlier.changePercent.toString()
                        text(": ${outlier.relayCount} relays ($change% compared to the surrounding days)")
                    }
                }
            }
        }
    }

    /** The timeline only depends on the selected day and the width */
    fun timelineQuery(view: LiteView) = buildString {
        view.day?.let { append("day=").append(it).append('&') }
        append("size=").append(view.size.parameter)
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
                "src" to "$MAP_PATH?${view.toImageQuery()}",
                "width" to view.size.width,
                "height" to view.size.height,
                "alt" to "Map of ${model.shownRelayCount} relays on $dayText. ${clickHint(view)}",
            )
        }
        tag("p", "class" to "summary") {
            text("Showing ${model.shownRelayCount} of ${model.totalRelayCount} relays on $dayText, zoom level ${view.zoom}. ${clickHint(view)}")
        }
    }

    private fun Html.selection(view: LiteView) {
        val selections = listOfNotNull(
            view.selectedFamily?.let { "Family $it" to view.copy(selectedFamily = null) },
            view.selectedAutonomousSystem?.let { "AS$it" to view.copy(selectedAutonomousSystem = null) },
            view.selectedCountry?.let { it to view.copy(selectedCountry = null) },
        )
        if (selections.isEmpty()) return
        tag("p", "class" to "selection") {
            text("Selected: ")
            selections.forEachIndexed { index, (label, cleared) ->
                if (index > 0) text(", ")
                text("$label (")
                tag("a", "clear", "href" to "$PATH?${cleared.toQuery()}")
                text(")")
            }
        }
    }

    private fun Html.statistics(statistics: LiteStatistics) {
        tag("table", "class" to "statistics") {
            tag("caption", "Shown relays")
            tag("tbody") {
                listOf(
                    "Exit relays" to statistics.exitCount,
                    "Guard relays" to statistics.guardCount,
                    "Other relays" to statistics.otherCount,
                    "Total relays" to statistics.totalCount,
                    "Families" to statistics.familyCount,
                    "Countries" to statistics.countryCount,
                    "Autonomous systems" to statistics.autonomousSystemCount,
                ).forEach { (label, count) ->
                    tag("tr") {
                        tag("th", label, "scope" to "row")
                        tag("td", count.toString())
                    }
                }
            }
        }
    }

    private fun Html.ranking(ranking: Ranking, view: LiteView) {
        tag("section") {
            tag("h2", ranking.title)
            if (ranking.rows.isEmpty()) {
                tag("p", "None of the shown relays has this information.")
                return@tag
            }
            tag("div", "class" to "table") {
                tag("table") {
                    tag("thead") {
                        tag("tr") { listOf("Name", "Relays", "").forEach { tag("th", it, "scope" to "col") } }
                    }
                    tag("tbody") {
                        ranking.rows.forEach { row ->
                            tag("tr") {
                                tag("td") {
                                    if (row.detailsPath != null) tag("a", row.label, "href" to row.detailsPath) else text(row.label)
                                }
                                tag("td", row.relayCount.toString())
                                tag("td") {
                                    if (row.selectView != view) tag("a", "Select on map", "href" to "$PATH?${row.selectView.toQuery()}")
                                    else text("Selected")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The countries with the most estimated Tor users and the color scale of the map, like the legend of the
     * JavaScript map's country usage setting
     */
    private fun Html.countryUsage(usage: List<CountryUsageDto>, countryNames: (String) -> String) {
        tag("section", "id" to "usage") {
            tag("h2", "Countries with the most Tor users")
            if (usage.isEmpty()) {
                tag("p", "Tor Metrics has no user estimates for this day. They exist from March 2011 on.")
                return@tag
            }
            tag("p", "class" to "usage-legend") {
                text("Few users ")
                tag("span", "class" to "usage-scale", "aria-hidden" to "true")
                text(" Many users")
            }
            val frac = usage.first().frac
            tag("p", "class" to "summary") {
                text("Estimates of Tor Metrics, based on $frac% of relays.")
                if (frac < LOW_ESTIMATE_FRACTION) text(" Early estimates are less reliable.")
            }
            tag("div", "class" to "table") {
                tag("table") {
                    tag("thead") {
                        tag("tr") { listOf("Country", "Estimated users", "Probably between").forEach { tag("th", it, "scope" to "col") } }
                    }
                    tag("tbody") {
                        usage.sortedByDescending { it.users }.take(USAGE_RANKING_SIZE).forEach { entry ->
                            tag("tr") {
                                tag("td", countryNames(entry.country))
                                tag("td", formatNumber(entry.users))
                                tag("td", if (entry.lower != null && entry.upper != null) "${formatNumber(entry.lower)} and ${formatNumber(entry.upper)}" else "")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun formatNumber(value: Int) = String.format(Locale.US, "%,d", value)

    private fun clickHint(view: LiteView) = when (view.clickMode) {
        ClickMode.List -> "Click the map to list the relays there."
        ClickMode.Zoom -> "Click the map to zoom in."
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
                tag("legend", "Group relays by")
                Grouping.entries.forEach { grouping ->
                    checkbox("group", grouping.parameter, grouping in view.groupings) { text(grouping.label) }
                }
            }
            tag("fieldset") {
                tag("legend", "Layers")
                checkbox("heat", "1", view.heatMap) { text("Heatmap of relays") }
                checkbox("usage", "1", view.countryUsage) { text("Color countries by estimated Tor users") }
            }
            tag("fieldset") {
                tag("legend", "Consensus weight")
                checkbox("weight", "1", view.sizeByWeight) { text("Size relay markers by consensus weight") }
                tag("label") {
                    text("Show top ")
                    tag("select", "name" to "top") {
                        tag("option", "All relays", "value" to "", "selected" to (view.topRelayCount == null))
                        LiteView.TOP_RELAY_COUNTS.forEach { count ->
                            tag("option", "$count relays", "value" to count, "selected" to (view.topRelayCount == count))
                        }
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
                tag("legend", "Click on the map")
                ClickMode.entries.forEach { mode ->
                    tag("label") {
                        tag("input", "type" to "radio", "name" to "click", "value" to mode.parameter, "checked" to (mode == view.clickMode))
                        text(" ${mode.label}")
                    }
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

    internal fun Html.hiddenInput(name: String, value: String) =
        tag("input", "type" to "hidden", "name" to name, "value" to value)
}
