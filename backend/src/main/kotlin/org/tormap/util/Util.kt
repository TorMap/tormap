package org.tormap.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun Instant.toLocalDate(): LocalDate = LocalDate.ofInstant(this, ZoneId.of("UTC"))

fun String.stripLengthForDB(maximumCharacters: Int = 255) = when (this.length) {
    in 0..maximumCharacters -> this
    else -> this.substring(0, maximumCharacters - 3) + "..."
}
