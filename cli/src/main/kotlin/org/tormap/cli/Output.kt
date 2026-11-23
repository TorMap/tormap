package org.tormap.cli

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.PrintStream

enum class Format { TABLE, JSON, NDJSON, CSV }

/**
 * A value from the API for a table cell or CSV field: every character that is not visible text ([isVisible]) is
 * replaced, including line breaks and tabs, which would fake rows or columns. Relay nicknames and contacts are chosen
 * by relay operators, and every other string comes from a server the user may not control (`--api-url`).
 * [TerminalSafePrintStream] enforces the same rule on all output; this keeps the rows and columns intact.
 */
fun sanitize(value: String): String =
    if (value.codePoints().allMatch(::isVisible)) value
    else buildString(value.length) {
        value.codePoints().forEach { if (isVisible(it)) appendCodePoint(it) else append(REPLACEMENT) }
    }

/**
 * JSON for the terminal: JSON encoders escape only C0 control characters, so every other character that is not
 * visible text ([isVisible]: DEL, C1 controls, bidirectional and other format characters, ...) is escaped as `\u`
 * here. Outside of strings, JSON has only visible characters and line breaks, and inside of strings the escapes keep
 * the value the same for any JSON parser, so [TerminalSafePrintStream] never has to replace a character of it.
 */
internal fun escapeControls(json: String): String =
    if (json.codePoints().allMatch { it == '\n'.code || isVisible(it) }) json
    else buildString(json.length + 16) {
        json.codePoints().forEach { codePoint ->
            if (codePoint == '\n'.code || isVisible(codePoint)) appendCodePoint(codePoint)
            else Character.toChars(codePoint).forEach { append("\\u%04x".format(it.code)) }
        }
    }

/** Text of a field for tables and CSV: arrays are joined with spaces, objects are shown by their fingerprint or as JSON. */
fun JsonObject.text(field: String): String = when (val value = this[field]) {
    null, JsonNull -> ""
    is JsonPrimitive -> value.content
    is JsonArray -> value.joinToString(" ") { element ->
        when (element) {
            is JsonPrimitive -> element.content
            is JsonObject -> element.text("fingerprint").ifEmpty { element.toString() }
            else -> element.toString()
        }
    }
    is JsonObject -> value.toString()
}

/**
 * Writes rows in the chosen format. JSON and NDJSON print the objects as the API returned them, with all fields;
 * tables show [tableColumns], CSV shows [csvColumns].
 */
class Output(private val out: PrintStream, private val format: Format) {
    private val prettyJson = Json { prettyPrint = true }

    fun rows(rows: List<JsonObject>, tableColumns: List<String>, csvColumns: List<String> = tableColumns) {
        when (format) {
            Format.JSON -> out.println(escapeControls(prettyJson.encodeToString(JsonElement.serializer(), JsonArray(rows))))
            Format.NDJSON -> rows.forEach { out.println(escapeControls(it.toString())) }
            Format.CSV -> {
                out.println(csvColumns.joinToString(",") { csv(it) })
                rows.forEach { row -> out.println(csvColumns.joinToString(",") { csv(row.text(it)) }) }
            }
            Format.TABLE -> table(tableColumns, rows.map { row -> tableColumns.map { row.text(it) } })
        }
    }

    /** One object: a two-column field/value table, or the object itself in JSON. */
    fun single(row: JsonObject, columns: List<String>) {
        when (format) {
            Format.JSON -> out.println(escapeControls(prettyJson.encodeToString(JsonElement.serializer(), row)))
            Format.NDJSON -> out.println(escapeControls(row.toString()))
            Format.CSV -> rows(listOf(row), columns)
            Format.TABLE -> table(listOf("field", "value"), columns.map { listOf(it, row.text(it)) }, header = false)
        }
    }

    /** Plain values, e.g. days: one per line, a JSON array of strings, or a one-column CSV. */
    fun values(name: String, values: List<String>) {
        when (format) {
            Format.JSON -> out.println(escapeControls(prettyJson.encodeToString(JsonElement.serializer(), JsonArray(values.map(::JsonPrimitive)))))
            Format.NDJSON -> values.forEach { out.println(escapeControls(JsonPrimitive(it).toString())) }
            Format.CSV -> {
                out.println(name)
                values.forEach { out.println(csv(it)) }
            }
            Format.TABLE -> values.forEach { out.println(sanitize(it)) }
        }
    }

    private fun table(columns: List<String>, cells: List<List<String>>, header: Boolean = true) {
        val lines = if (header) listOf(columns) + cells else cells
        val widths = columns.indices.map { column -> lines.maxOfOrNull { it[column].length } ?: 0 }
        // Numbers are right-aligned, so they can be compared at a glance
        val numeric = columns.indices.map { column -> cells.isNotEmpty() && cells.all { it[column].isEmpty() || it[column].toDoubleOrNull() != null } }
        lines.forEach { line ->
            out.println(line.indices.joinToString("  ") { column ->
                val cell = sanitize(line[column])
                if (numeric[column]) cell.padStart(widths[column]) else cell.padEnd(widths[column])
            }.trimEnd())
        }
    }


    private fun csv(value: String): String {
        val safe = sanitize(value)
        // A leading =, +, - or @ would be run as a formula by spreadsheet programs
        val defused = if (safe.firstOrNull() in setOf('=', '+', '-', '@') && safe.toDoubleOrNull() == null) "'$safe" else safe
        return if (defused.any { it == ',' || it == '"' || it == '\n' || it == '\r' } || defused != value) "\"${defused.replace("\"", "\"\"")}\"" else defused
    }
}
