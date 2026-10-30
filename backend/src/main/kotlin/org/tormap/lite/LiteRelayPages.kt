package org.tormap.lite

import org.tormap.adapter.dto.RelayDetailsDto
import org.tormap.adapter.dto.RelayLocationDto
import org.tormap.database.entity.RelayDetails
import org.tormap.lite.LitePage.backToMap
import org.tormap.lite.LitePage.relayTable
import org.tormap.lite.LitePage.searchForm
import java.util.Locale

/**
 * The Lite pages about relays: details of one relay, a family, an autonomous system and search results.
 * Relay-published values (nickname, contact, platform, ...) are only written as escaped text. Links built from relay
 * data (fingerprint, IP address) are only written after checking that the value has the expected format.
 */
object LiteRelayPages {
    private val fingerprintPattern = Regex("^[0-9A-F]{40}$")
    private val addressPattern = Regex("^[0-9A-Fa-f:.]{2,45}$")

    fun renderRelay(details: RelayDetailsDto, location: RelayLocationDto?, view: LiteView, fullVersionUrl: String): String =
        LitePage.layout("${details.nickname} - TorMap Lite", view, fullVersionUrl) {
            backToMap(view)
            tag("h2", "Relay ${details.nickname}")
            val query = view.toQuery()
            val rows = listOf<Pair<String, (Html.() -> Unit)?>>(
                "Fingerprint" to { metricsLink(details.fingerprint, details.fingerprint.takeIf(fingerprintPattern::matches)?.let { "details/$it" }) },
                "IP address" to { metricsLink(details.address, details.address.takeIf(addressPattern::matches)?.let { "search/$it" }) },
                "Flags assigned by authorities" to location?.let { { text(LiteRelays.flagNames(it.flags).joinToString(", ").ifEmpty { "no flags assigned" }) } },
                "Autonomous system" to details.autonomousSystemNumber?.let { number ->
                    {
                        tag("a", "${details.autonomousSystemName ?: "Unknown"} (AS$number)", "href" to "${LitePage.AUTONOMOUS_SYSTEM_PATH}$number?$query")
                    }
                },
                "Platform" to details.platform?.let { { text(it) } },
                "Uptime" to details.uptime?.let { { text(String.format(Locale.ROOT, "%.2f hours", it / 3600.0)) } },
                "Contact" to details.contact?.let { { text(it) } },
                "Bandwidth for short intervals" to megabytesPerSecond(details.bandwidthBurst),
                "Bandwidth for long periods" to megabytesPerSecond(details.bandwidthRate),
                "Bandwidth observed" to megabytesPerSecond(details.bandwidthObserved),
                "Supported protocols" to details.protocols?.let { { text(it) } },
                "Allows single hop exit" to yesNo(details.allowSingleHopExits),
                "Is hibernating" to yesNo(details.isHibernating),
                "Caches extra info" to yesNo(details.cachesExtraInfo),
                "Is a hidden service directory" to yesNo(details.isHiddenServiceDir),
                "Accepts tunneled directory requests" to yesNo(details.tunnelledDirServer),
                "Link protocol versions" to details.linkProtocolVersions?.let { { text(it) } },
                "Circuit protocol versions" to details.circuitProtocolVersions?.let { { text(it) } },
                "Self reported family members" to details.familyEntries?.let { { text(it) } },
                "Confirmed family members" to details.confirmedFamilyMembers.takeIf { it.isNotEmpty() }?.let { members ->
                    {
                        tag("ul", "class" to "plain") {
                            members.forEach { member ->
                                tag("li") { tag("a", "${member.nickname} (${member.fingerprint})", "href" to "${LitePage.RELAY_PATH}${member.id}?$query") }
                            }
                        }
                        details.familyId?.let { familyId ->
                            tag("a", "Show the whole family", "href" to "${LitePage.FAMILY_PATH}$familyId?$query")
                            text(" \u00b7 ")
                            tag("a", "Show it on the map", "href" to "${LitePage.PATH}?${view.showGroup(Grouping.Family).copy(selectedFamily = familyId).toQuery()}")
                        }
                    }
                },
                "Verified host names" to hostNames(details.verifiedHostNames),
                "Unverified host names" to hostNames(details.unverifiedHostNames),
                "Infos published by relay on" to { text(details.day.toString()) },
            )
            tag("table", "class" to "details") {
                tag("tbody") {
                    rows.forEach { (name, value) ->
                        if (value != null) {
                            tag("tr") {
                                tag("th", name, "scope" to "row")
                                tag("td") { value() }
                            }
                        }
                    }
                }
            }
        }

    fun renderFamily(familyId: Long, members: List<RelayDetails>, view: LiteView, fullVersionUrl: String): String =
        LitePage.layout("Family - TorMap Lite", view, fullVersionUrl) {
            backToMap(view)
            tag("h2", "Family of ${members.size} relays")
            tag("p") { tag("a", "Show this family on the map", "href" to "${LitePage.PATH}?${view.showGroup(Grouping.Family).copy(selectedFamily = familyId).toQuery()}") }
            tag("p", "class" to "summary") {
                text("Relays that list each other as family members in ${members.firstOrNull()?.month ?: "this month"} (family $familyId).")
            }
            val query = view.toQuery()
            tag("div", "class" to "table") {
                tag("table") {
                    tag("thead") {
                        tag("tr") { listOf("Nickname", "Fingerprint", "Autonomous system").forEach { tag("th", it, "scope" to "col") } }
                    }
                    tag("tbody") {
                        members.sortedBy { it.nickname.lowercase() }.forEach { member ->
                            tag("tr") {
                                tag("td") { tag("a", member.nickname, "href" to "${LitePage.RELAY_PATH}${member.id}?$query") }
                                tag("td", member.fingerprint, "class" to "fingerprint")
                                tag("td") {
                                    member.autonomousSystemNumber?.let { number ->
                                        tag("a", "${member.autonomousSystemName ?: "Unknown"} (AS$number)", "href" to "${LitePage.AUTONOMOUS_SYSTEM_PATH}$number?$query")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    fun renderAutonomousSystem(number: Int, name: String?, relays: List<RelayLocationDto>, view: LiteView, fullVersionUrl: String): String =
        LitePage.layout("AS$number - TorMap Lite", view, fullVersionUrl) {
            backToMap(view)
            tag("h2", "${name ?: "Autonomous system"} (AS$number)")
            tag("p", "class" to "summary") { text("Relays in this autonomous system on ${view.day ?: "no day"}.") }
            if (relays.isNotEmpty()) {
                tag("p") {
                    tag("a", "Show this autonomous system on the map", "href" to "${LitePage.PATH}?${view.showGroup(Grouping.AutonomousSystem).copy(selectedAutonomousSystem = number).toQuery()}")
                }
            }
            if (relays.isEmpty()) tag("p", "No relays of this autonomous system are shown on this day.")
            else relayTable(relays, view)
        }

    fun renderSearch(query: String, relays: List<RelayLocationDto>, view: LiteView, fullVersionUrl: String): String =
        LitePage.layout("Search - TorMap Lite", view, fullVersionUrl) {
            backToMap(view)
            searchForm(view, query)
            tag("h2", "Relays matching “$query”")
            if (relays.isEmpty()) tag("p", "No relay of ${view.day ?: "this day"} matches. Search for a part of a nickname or a whole fingerprint.")
            else relayTable(relays, view)
        }

    fun renderNotFound(message: String, view: LiteView, fullVersionUrl: String): String =
        LitePage.layout("Not found - TorMap Lite", view, fullVersionUrl) {
            backToMap(view)
            tag("h2", "Not found")
            tag("p", message)
        }

    /**
     * Writes a value with a link to Tor Metrics Relay Search, or as plain text if it has no valid link target
     */
    private fun Html.metricsLink(label: String, fragment: String?) {
        if (fragment == null) text(label)
        else tag("a", label, "href" to "https://metrics.torproject.org/rs.html#$fragment")
    }

    private fun megabytesPerSecond(bytes: Int): (Html.() -> Unit)? =
        bytes.takeIf { it > 0 }?.let { { text(String.format(Locale.ROOT, "%.2f MB/s", it / 1_000_000.0)) } }

    private fun yesNo(value: Boolean): Html.() -> Unit = { text(if (value) "yes" else "no") }

    private fun hostNames(names: Collection<String>?): (Html.() -> Unit)? = names?.takeIf { it.isNotEmpty() }?.let {
        { tag("ul", "class" to "plain") { it.forEach { name -> tag("li", name) } } }
    }
}
