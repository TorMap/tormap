package org.tormap.adapter.dto

import java.time.LocalDate

/**
 * The number of relays of a single day, split by the relay type the frontend displays.
 * A relay with the Exit flag counts as exit, otherwise a relay with the Guard flag counts as guard.
 */
class DailyRelayCountDto(
    val day: LocalDate,
    val total: Long,
    val exit: Long,
    val guard: Long,
) {
    val other: Long = total - exit - guard
}
