package org.tormap.util

import org.tormap.database.entity.RelayDetails

/**
 * Check to which family the [requestingRelay] and the [newMember] belong
 */
fun MutableList<Set<RelayDetails>>.addFamilyMember(
    requestingRelay: RelayDetails,
    newMember: RelayDetails
) {
    val requestingRelayIndex = this.indexOfFirst { it.contains(requestingRelay) }
    val newMemberIndex = this.indexOfFirst { it.contains(newMember) }

    if (
        requestingRelayIndex >= 0 &&
        newMemberIndex >= 0 &&
        requestingRelayIndex != newMemberIndex
    ) {
        this[requestingRelayIndex] = this[requestingRelayIndex].plus(this[newMemberIndex])
        this.removeAt(newMemberIndex)
    } else if (requestingRelayIndex >= 0) {
        this[requestingRelayIndex] = this[requestingRelayIndex].plus(newMember)
    } else if (newMemberIndex >= 0) {
        this[newMemberIndex] = this[newMemberIndex].plus(requestingRelay)
    } else {
        this.add(setOf(requestingRelay, newMember))
    }
}

/**
 * Extract the fingerprint of a [familyEntry] and check if it is a family member of this [RelayDetails]
 */
fun RelayDetails.getFamilyMember(
    familyEntry: String,
    relaysWithFamilyEntries: List<RelayDetails>
): RelayDetails? {
    when {
        familyEntry.matches(familyEntryFingerprintRegex) -> {
            val fingerprint = extractFingerprintFromFamilyEntry(familyEntry)
            val allegedFamilyMember = relaysWithFamilyEntries.find { it.fingerprint == fingerprint }
            if (allegedFamilyMember != null && allegedFamilyMember.confirmsFamilyMember(this)) {
                return allegedFamilyMember
            }
        }
        familyEntry.matches(familyEntryNicknameRegex) -> {
            relaysWithFamilyEntries.filter { it.nickname == familyEntry }
                .firstOrNull { it.confirmsFamilyMember(this) }
        }
        else -> throw Exception("Format of alleged family member $familyEntry for requestingRelay ${this.fingerprint} not supported!")
    }
    return null
}

/**
 * Determines if this [RelayDetails] shares a family with the [allegedFamilyMember].
 */
fun RelayDetails.confirmsFamilyMember(
    allegedFamilyMember: RelayDetails
): Boolean = this.familyEntries?.commaSeparatedToList()?.any {
    when {
        familyEntryFingerprintRegex.matches(it) -> {
            allegedFamilyMember.fingerprint == extractFingerprintFromFamilyEntry(it)
        }
        familyEntryNicknameRegex.matches(it) -> {
            allegedFamilyMember.nickname == it
        }
        else -> false
    }
} ?: false

private fun extractFingerprintFromFamilyEntry(familyEntry: String) = familyEntry.substring(1, 41)

private val familyEntryFingerprintRegex = Regex("^\\$[A-F0-9]{40}$")
private val familyEntryNicknameRegex = Regex("^[a-zA-Z0-9]{1,19}$")
