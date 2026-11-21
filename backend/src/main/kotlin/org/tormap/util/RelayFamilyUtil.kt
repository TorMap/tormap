package org.tormap.util

import org.tormap.database.entity.RelayDetails

/**
 * The families found by [computeRelayFamilies].
 * @param families relays that are connected through confirmed family entries or shared family IDs,
 * each family has at least two members
 * @param confirmedConnectionCount family line entries that are confirmed by the listed relay
 * @param rejectedConnectionCount family line entries that are not confirmed or invalid
 * @param relaysWithFamilyIdCount relays with at least one family ID from a family certificate
 */
class RelayFamilies(
    val families: List<List<RelayDetails>>,
    val confirmedConnectionCount: Int,
    val rejectedConnectionCount: Int,
    val relaysWithFamilyIdCount: Int,
)

/**
 * Groups [relays] into families like Tor clients do (proposal 321). Two relays are connected if
 * - they list each other by fingerprint in their family lines ([RelayDetails.familyEntries]), or
 * - they share a family ID from a verified family certificate ([RelayDetails.familyIds]).
 *
 * Nickname entries are ignored like in Tor and Onionoo, because nicknames are not unique.
 * Connected relays form one family, also when they are only connected through other members.
 *
 * Relays are looked up by fingerprint and the families are tracked with a union-find, so this runs in time linear
 * to the number of family entries. It must not put [RelayDetails] into hash based collections, because their
 * [RelayDetails.hashCode] is constant.
 *
 * @param onInvalidEntry called for family entries that have an unsupported format
 */
fun computeRelayFamilies(
    relays: List<RelayDetails>,
    onInvalidEntry: (relay: RelayDetails, familyEntry: String) -> Unit = { _, _ -> },
): RelayFamilies {
    val declaredFamilies = relays.map { DeclaredFamily.of(it.familyEntries) }
    val indexByFingerprint = HashMap<String, Int>(relays.size * 2)
    relays.forEachIndexed { index, relay -> indexByFingerprint.putIfAbsent(relay.fingerprint, index) }
    val parents = IntArray(relays.size) { it }
    val hasMember = BooleanArray(relays.size)
    var confirmedConnectionCount = 0
    var rejectedConnectionCount = 0

    fun root(index: Int): Int {
        var root = index
        while (parents[root] != root) {
            parents[root] = parents[parents[root]]
            root = parents[root]
        }
        return root
    }

    fun connect(index: Int, otherIndex: Int) {
        parents[root(otherIndex)] = root(index)
        hasMember[otherIndex] = true
        hasMember[index] = true
    }

    relays.forEachIndexed { requestingIndex, requestingRelay ->
        declaredFamilies[requestingIndex].entries.forEach { entry ->
            when (entry) {
                is FamilyEntry.Fingerprint -> {
                    val memberIndex = indexByFingerprint[entry.fingerprint]
                    if (
                        memberIndex != null
                        && memberIndex != requestingIndex
                        && declaredFamilies[memberIndex].confirms(requestingRelay)
                    ) {
                        connect(requestingIndex, memberIndex)
                        confirmedConnectionCount++
                    } else {
                        rejectedConnectionCount++
                    }
                }
                is FamilyEntry.Nickname -> rejectedConnectionCount++
                is FamilyEntry.Invalid -> {
                    onInvalidEntry(requestingRelay, entry.value)
                    rejectedConnectionCount++
                }
            }
        }
    }

    val firstIndexByFamilyId = HashMap<String, Int>()
    var relaysWithFamilyIdCount = 0
    relays.forEachIndexed { index, relay ->
        val familyIds = relay.familyIds?.commaSeparatedToList()?.filter { it.isNotEmpty() } ?: emptyList()
        if (familyIds.isNotEmpty()) relaysWithFamilyIdCount++
        familyIds.forEach { familyId ->
            val firstIndex = firstIndexByFamilyId.putIfAbsent(familyId, index)
            if (firstIndex != null && firstIndex != index) connect(firstIndex, index)
        }
    }

    val familyIndexByRoot = HashMap<Int, Int>()
    val families = mutableListOf<MutableList<RelayDetails>>()
    relays.forEachIndexed { index, relay ->
        if (hasMember[index]) {
            val familyIndex = familyIndexByRoot.getOrPut(root(index)) {
                families.add(mutableListOf())
                families.size - 1
            }
            families[familyIndex].add(relay)
        }
    }
    return RelayFamilies(families, confirmedConnectionCount, rejectedConnectionCount, relaysWithFamilyIdCount)
}

private sealed interface FamilyEntry {
    class Fingerprint(val fingerprint: String) : FamilyEntry
    object Nickname : FamilyEntry
    class Invalid(val value: String) : FamilyEntry
}

/**
 * The parsed family entries of a relay
 */
private class DeclaredFamily(val entries: List<FamilyEntry>) {
    private val fingerprints = entries.filterIsInstance<FamilyEntry.Fingerprint>().map { it.fingerprint }.toHashSet()

    fun confirms(allegedFamilyMember: RelayDetails) = allegedFamilyMember.fingerprint in fingerprints

    companion object {
        fun of(familyEntries: String?) = DeclaredFamily(
            familyEntries?.commaSeparatedToList()?.map {
                when {
                    familyEntryFingerprintRegex.matches(it) -> FamilyEntry.Fingerprint(it.substring(1, 41))
                    familyEntryNicknameRegex.matches(it) -> FamilyEntry.Nickname
                    else -> FamilyEntry.Invalid(it)
                }
            } ?: emptyList()
        )
    }
}

private val familyEntryFingerprintRegex = Regex("^\\$[A-F0-9]{40}$")
private val familyEntryNicknameRegex = Regex("^[a-zA-Z0-9]{1,19}$")
