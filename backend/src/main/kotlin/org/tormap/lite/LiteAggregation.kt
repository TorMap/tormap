package org.tormap.lite

import org.tormap.adapter.dto.RelayLocationDto

/**
 * The relay counts shown below the map, like the statistics panel of the JavaScript map
 */
data class LiteStatistics(
    val exitCount: Int,
    val guardCount: Int,
    val otherCount: Int,
    val familyCount: Int,
    val countryCount: Int,
    val autonomousSystemCount: Int,
) {
    val totalCount get() = exitCount + guardCount + otherCount
}

/**
 * The relays of one group (e.g. a family) at one location, drawn as a circle. [padding] makes the circles of the
 * larger groups at the same location a little bigger, so no two circles there have the same size.
 */
data class GroupAtLocation(val groupId: Long, val relays: List<RelayLocationDto>, val padding: Int)

/**
 * Filtering, grouping and counting relays like the JavaScript map does (frontend `util/aggregate-relays.ts`), so both
 * show the same relays and numbers for the same settings
 */
object LiteAggregation {
    /**
     * Returns the relays that match the relay types and required flags of the view, limited to the relays with the
     * highest consensus weight if the view has a limit, like filterRelaysByFlags()
     */
    fun filterRelays(relays: List<RelayLocationDto>, view: LiteView): List<RelayLocationDto> {
        val requiredFlags = view.requiredFlags.map { it.ordinal }
        val filtered = relays.filter { relay ->
            LiteRelayType.of(relay.flags) in view.relayTypes && relay.flags.orEmpty().containsAll(requiredFlags)
        }
        return keepRelaysWithHighestWeight(filtered, if (view.sizeByWeight) view.topRelayCount else null)
    }

    /**
     * Returns the [count] relays with the highest consensus weight. Relays are returned unchanged without a count or
     * if none of them has a weight (e.g. before the backend processed the weights of a day).
     */
    fun keepRelaysWithHighestWeight(relays: List<RelayLocationDto>, count: Int?): List<RelayLocationDto> {
        if (count == null || relays.size <= count) return relays
        if (relays.none { it.consensusWeight != null }) return relays
        // sortedByDescending is stable, so relays with the same weight keep their order
        return relays.sortedByDescending { it.consensusWeight ?: -1 }.take(count)
    }

    /**
     * Removes selections of groups that have no relays on the view's day, like the JavaScript map does
     */
    fun normalized(view: LiteView, relays: List<RelayLocationDto>): LiteView = view.copy(
        selectedFamily = view.selectedFamily?.takeIf { id -> relays.any { it.familyId == id } },
        selectedAutonomousSystem = view.selectedAutonomousSystem?.takeIf { number -> relays.any { it.asNumber == number } },
        selectedCountry = view.selectedCountry?.takeIf { country -> relays.any { it.country == country } },
    )

    /**
     * Returns the relays of the selected country, family and autonomous system, like buildFilteredRelays()
     */
    fun selectedRelays(relays: List<RelayLocationDto>, view: LiteView): List<RelayLocationDto> = relays.filter { relay ->
        (view.selectedCountry == null || relay.country == view.selectedCountry) &&
            (view.selectedFamily == null || relay.familyId == view.selectedFamily) &&
            (view.selectedAutonomousSystem == null || relay.asNumber == view.selectedAutonomousSystem)
    }

    /**
     * Counts the relays by type and their groups, like buildStatistics()
     * @param relays - the relays after [filterRelays]
     */
    fun statistics(relays: List<RelayLocationDto>, view: LiteView): LiteStatistics {
        val selected = selectedRelays(relays, view)
        val types = selected.groupingBy { LiteRelayType.of(it.flags) }.eachCount()
        return LiteStatistics(
            exitCount = types[LiteRelayType.Exit] ?: 0,
            guardCount = types[LiteRelayType.Guard] ?: 0,
            otherCount = types[LiteRelayType.Other] ?: 0,
            familyCount = selected.mapNotNull { it.familyId }.toSet().size,
            countryCount = selected.map { it.country }.toSet().size,
            autonomousSystemCount = selected.mapNotNull { it.asNumber }.toSet().size,
        )
    }

    /**
     * Returns the relays at each location, keyed by latitude and longitude
     */
    fun byLocation(relays: List<RelayLocationDto>): Map<Pair<Double, Double>, List<RelayLocationDto>> =
        relays.groupBy { it.lat.toDouble() to it.long.toDouble() }

    /**
     * Returns the groups at each location, the largest first, like sortGroupCoordinatesMap(). Relays without a group
     * are left out.
     */
    fun groupsByLocation(
        relays: List<RelayLocationDto>,
        groupOf: (RelayLocationDto) -> Long?,
    ): Map<Pair<Double, Double>, List<GroupAtLocation>> = byLocation(relays).mapValues { (_, relaysAtLocation) ->
        val groups = relaysAtLocation
            .mapNotNull { relay -> groupOf(relay)?.let { it to relay } }
            .groupBy({ it.first }, { it.second })
            .entries
            .sortedByDescending { it.value.size }
        // The same padding as the frontend: a group gets its own size and the sizes of all smaller groups except the
        // smallest, the smallest group gets none
        groups.mapIndexed { index, (groupId, groupRelays) ->
            val padding = if (index < groups.lastIndex) groups.subList(index, groups.lastIndex).sumOf { it.value.size } else 0
            GroupAtLocation(groupId, groupRelays, padding)
        }
    }.filterValues { it.isNotEmpty() }

    /**
     * Returns the groups with the most relays, e.g. for a ranking below the map
     */
    fun largestGroups(relays: List<RelayLocationDto>, groupOf: (RelayLocationDto) -> Any?, limit: Int): List<Pair<Any, Int>> =
        relays.mapNotNull(groupOf).groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
}
