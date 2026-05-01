package org.tormap.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Get logger for any class
 */
@Suppress("unused")
inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)

fun <T> Iterable<T>.jointToCommaSeparated() = this.joinToString(", ")

fun String.commaSeparatedToList() = this.split(",").map { it.trim() }

fun millisSinceEpochToLocalDate(millisSinceEpoch: Long): LocalDate = LocalDate.ofInstant(
    Instant.ofEpochMilli(millisSinceEpoch),
    ZoneId.of("UTC")
)

fun String?.stripLengthForDB(maximumCharacters: Int = 255) = when {
    this == null || this.length <= maximumCharacters -> this
    else -> this.substring(0, maximumCharacters - 3) + "..."
}