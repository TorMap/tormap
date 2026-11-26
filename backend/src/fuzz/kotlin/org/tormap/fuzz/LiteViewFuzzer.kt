package org.tormap.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import org.tormap.lite.LiteView

/**
 * TorMap Lite builds its view from untrusted query parameters. Parsing and navigating must not fail, and the links it
 * generates must lead to the same view, so they don't create more distinct pages and map images than needed.
 */
object LiteViewFuzzer {
    private val names = listOf(
        "zoom", "lat", "lng", "size", "click", "f", "type", "flag", "day", "group", "weight", "top", "family", "as",
        "country", "heat", "usage",
    )

    @JvmStatic
    fun fuzzerTestOneInput(data: FuzzedDataProvider) {
        val parameters = mutableMapOf<String, MutableList<String>>()
        repeat(data.consumeInt(0, 20)) {
            parameters.getOrPut(data.pickValue(names)) { mutableListOf() }.add(data.consumeString(40))
        }
        var view = LiteView.parse(parameters)
        view = when (data.consumeInt(0, 3)) {
            0 -> view.zoomedBy(data.consumeInt(-20, 20))
            1 -> view.zoomedInAt(data.consumeInt(), data.consumeInt())
            2 -> view.panned(data.consumeRegularDouble(), data.consumeRegularDouble())
            else -> view
        }
        val query = view.toQuery()
        view.toImageQuery()
        view.toPermalinkHash()
        val reparsed = LiteView.parse(view.queryParameters().groupBy({ it.first }, { it.second }))
        check(reparsed.toQuery() == query) { "Link $query leads to ${reparsed.toQuery()}" }
    }
}
