package org.tormap.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun <T> Iterable<T>.jointToCommaSeparated() = this.joinToString(", ")

fun String.commaSeparatedToList() = this.split(",").map { it.trim() }

fun millisSinceEpochToLocalDate(millisSinceEpoch: Long): LocalDate = LocalDate.ofInstant(
    Instant.ofEpochMilli(millisSinceEpoch),
    ZoneId.of("UTC")
)

fun String?.stripLengthForDB(maximumCharacters: Int = 255) = when (this?.length) {
    null, in 0..maximumCharacters -> this
    else -> this.substring(0, maximumCharacters - 3) + "..."
}
