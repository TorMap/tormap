package org.tormap.util

/**
 * Helpers for Tor's path selection, see section 3.8.3 of
 * [dir-spec](https://spec.torproject.org/dir-spec/computing-consensus.html#bandwidth-weights).
 */
object PathSelection {
    /**
     * The weights of a consensus (e.g. `Wgg=5000`) are fractions of this scale.
     */
    const val BANDWIDTH_WEIGHT_SCALE = 10_000

    /**
     * Returns the share of a relay's consensus weight that clients use for choosing it as entry guard, so the
     * relay's probability of being picked as guard is `guardWeight / sum of the guard weights of all relays`.
     *
     * - A relay that is not `Running` and `Valid` can not be picked, which results in 0.
     * - Guard flagged relays use `Wgg`, relays that are also exits use `Wgd`, all other relays `Wgm`.
     *
     * @param consensusWeight the `w Bandwidth=` value of the relay's consensus entry
     * @param flags the names of the flags the relay has in the consensus
     * @param bandwidthWeights the `bandwidth-weights` of the consensus, null if it has none
     * @return null, if the consensus does not contain the required weight
     */
    fun guardWeight(consensusWeight: Long, flags: Set<String>, bandwidthWeights: Map<String, Int>?): Long? {
        if (!flags.contains("Running") || !flags.contains("Valid")) return 0
        val weightKey = when {
            flags.contains("Guard") && flags.contains("Exit") -> "Wgd"
            flags.contains("Guard") -> "Wgg"
            else -> "Wgm"
        }
        val weight = bandwidthWeights?.get(weightKey) ?: return null
        return Math.round(consensusWeight.toDouble() * weight / BANDWIDTH_WEIGHT_SCALE)
    }
}
