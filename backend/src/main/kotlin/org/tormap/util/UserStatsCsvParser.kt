package org.tormap.util

import org.tormap.adapter.dto.CountryUsageRow
import java.time.LocalDate

/**
 * Parses the CSV of Tor Metrics' relay users per country (`date,country,users,lower,upper,frac`).
 * The input is untrusted, so every line is validated and invalid lines are skipped instead of failing the import.
 */
object UserStatsCsvParser {
    private const val COLUMN_COUNT = 6
    private val countryPattern = Regex("^[a-z]{2}$")

    private sealed interface ParsedLine {
        class Row(val row: CountryUsageRow) : ParsedLine

        /** A valid line that is not about a country, e.g. a comment, the header or the global total */
        data object Ignored : ParsedLine

        data object Malformed : ParsedLine
    }

    /**
     * Returns the rows of countries. Comments, the header, the global total (empty country), unknown countries (`??`)
     * and legacy codes such as `a1` are skipped silently.
     * @param onMalformedLine called for every line that is invalid
     */
    fun parse(lines: Sequence<String>, onMalformedLine: () -> Unit = {}): Sequence<CountryUsageRow> =
        lines.mapNotNull { line ->
            when (val parsed = parseLine(line)) {
                is ParsedLine.Row -> parsed.row
                ParsedLine.Ignored -> null
                ParsedLine.Malformed -> {
                    onMalformedLine()
                    null
                }
            }
        }

    private fun parseLine(line: String): ParsedLine {
        if (line.isBlank() || line.startsWith("#") || line.startsWith("date,")) return ParsedLine.Ignored
        val columns = line.split(",")
        if (columns.size != COLUMN_COUNT) return ParsedLine.Malformed
        val country = columns[1]
        if (!countryPattern.matches(country)) {
            // The global total has no country, other values are unknown or legacy codes. Any other value is invalid.
            val isKnownNonCountry = country.isEmpty() || country == "??" || country.matches(Regex("^[a-z0-9]{2}$"))
            return if (isKnownNonCountry) ParsedLine.Ignored else ParsedLine.Malformed
        }
        return try {
            val users = columns[2].toInt()
            val frac = columns[5].toInt()
            if (users < 0 || frac !in 0..100) return ParsedLine.Malformed
            ParsedLine.Row(
                CountryUsageRow(
                    day = LocalDate.parse(columns[0]),
                    countryCode = country.uppercase(),
                    users = users,
                    lower = columns[3].takeIf { it.isNotEmpty() }?.toInt()?.takeIf { it >= 0 },
                    upper = columns[4].takeIf { it.isNotEmpty() }?.toInt()?.takeIf { it >= 0 },
                    frac = frac,
                )
            )
        } catch (exception: Exception) {
            ParsedLine.Malformed
        }
    }
}
