package org.tormap

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

fun String?.stripLengthForDB() = when {
        this == null || this.length <= 255 -> this
        else -> this.substring(0, 252) + "..."
    }

/**
 * Transform a string representation of an [ipv4Address] to a number representation
 */
fun calculateIPv4NumberRepresentation(ipv4Address: String): Long {
    val ipAddressInArray = ipv4Address.split(".").toTypedArray()
    var result: Long = 0
    var ip: Long
    for (x in 3 downTo 0) {
        ip = ipAddressInArray[3 - x].toLong()
        result = result or (ip shl (x shl 3))
    }
    return result
}
