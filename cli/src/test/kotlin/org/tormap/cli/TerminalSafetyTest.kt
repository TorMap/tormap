package org.tormap.cli

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/** What may reach the terminal: visible text, line feeds, tabs, and carriage returns only as part of CRLF. */
private fun forbiddenIn(text: String): List<String> {
    val found = mutableListOf<String>()
    text.codePoints().toArray().forEachIndexed { index, codePoint ->
        val crlf = codePoint == '\r'.code && text.codePoints().skip(index + 1L).findFirst().orElse(-1) == '\n'.code
        if (!(crlf || codePoint == '\n'.code || codePoint == '\t'.code || isVisible(codePoint))) found += "U+%04X".format(codePoint)
    }
    return found
}

private fun filtered(write: (PrintStream) -> Unit): String {
    val bytes = ByteArrayOutputStream()
    TerminalSafePrintStream.wrap(PrintStream(bytes, true, Charsets.UTF_8)).use(write)
    return bytes.toString(Charsets.UTF_8)
}

/**
 * Every character a hostile server or relay operator could use against a terminal or a reader: escape sequences (OSC
 * title, screen clear, the single-character C1 CSI), BEL, DEL, bidirectional overrides and isolates, zero-width and
 * soft hyphen, a line separator, CR, LF, tab, a Unicode tag character and a lone surrogate. As JSON string escapes.
 */
private const val POISON =
    "\\u001b]0;pwned\\u0007\\u001b[2J\\u009b31m\\u007f\\u202e\\u2066\\u200b\\u00ad\\u2028\\r\\n\\t\\udb40\\udc41\\ud800"

class TerminalSafetyTest : StringSpec({
    "no code point that is a control, format, separator, private-use, unassigned or surrogate is visible" {
        val invisibleTypes = setOf(
            Character.CONTROL, Character.FORMAT, Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR,
            Character.PRIVATE_USE, Character.UNASSIGNED, Character.SURROGATE,
        ).map { it.toInt() }
        (0..Character.MAX_CODE_POINT).filter { isVisible(it) && Character.getType(it) in invisibleTypes } shouldBe emptyList()
    }

    "every code point written to the terminal comes out visible, as a line break or tab, or replaced" {
        val all = buildString { (0..Character.MAX_CODE_POINT).forEach { appendCodePoint(it) } }
        forbiddenIn(filtered { it.print(all) }).shouldBeEmpty()
    }

    "visible text passes unchanged, also when its bytes arrive one by one" {
        val text = "alpha Ünïcødé 中文 العربية 😀 é € ≥ «»\ttab\nnext\r\nwindows\n"
        filtered { it.print(text) } shouldBe text
        filtered { out -> text.toByteArray(Charsets.UTF_8).forEach { out.write(it.toInt()) } } shouldBe text
    }

    "control and format characters are replaced, a CR only outside of CRLF" {
        filtered { it.print("a\u001b[2Jb\u009bc\u007fd‮e⁦f​g­h i\rj\u0007") } shouldBe "a?[2Jb?c?d?e?f?g?h?i?j?"
        filtered { it.print("a\ud800b\udc00c") } shouldBe "a?b?c"
        filtered { it.print("ends with CR\r") } shouldBe "ends with CR?"
    }

    "wrapping twice filters once" {
        val bytes = ByteArrayOutputStream()
        val once = TerminalSafePrintStream.wrap(PrintStream(bytes, true, Charsets.UTF_8))
        TerminalSafePrintStream.wrap(once) shouldBe once
    }

    "sanitize and escapeControls follow the same rule" {
        sanitize("be‮ta\nx\ty") shouldBe "be?ta?x?y"
        val json = escapeControls("{\"n\":\"a‮b\u009bc😀󠁁\"}\n")
        json shouldBe "{\"n\":\"a\\u202eb\\u009bc😀\\udb40\\udc41\"}\n"
        Json.parseToJsonElement(json).jsonObject["n"]!!.jsonPrimitive.content shouldBe "a‮b\u009bc😀󠁁"
    }

    "no command and format writes a forbidden character, whatever the API returns" {
        FakeApi().use { api ->
            val p = POISON
            // The poisoned nickname as the API returns it, decoded
            val nickname = Json.parseToJsonElement("\"a$POISON\"").jsonPrimitive.content
            api.route("/v1/days", """["2024-03-04","2024-03-05"]""")
            api.route(
                "/v1/days/2024-03-05/relays",
                """[{"fingerprint":"$FP_A","nickname":"a$p","country":"D$p","flags":["Exit$p","Running"],"consensusWeight":3,
                    "autonomousSystemNumber":1,"familyId":7,"unknown$p":"v$p"},
                   {"fingerprint":"$FP_B","nickname":"a$p","country":"NL","flags":["Running"],"consensusWeight":2,"familyId":7}]""",
            )
            val details = """{"fingerprint":"$FP_A","nickname":"a$p","month":"2024-03","day":"2024-03-05","address":"192.0.2.1$p",
                "autonomousSystemName":"AS$p","platform":"Tor$p","contact":"=c$p","familyId":7,
                "confirmedFamilyMembers":[{"fingerprint":"$FP_B$p","nickname":"b$p"}],"verifiedHostNames":["h$p"],"x$p":{"y$p":"z$p"}}"""
            api.route("/v1/relays/$FP_A", details)
            api.route("/v1/relays/$FP_A?month=2024-03", details)
            api.route("/v1/relays/$FP_A/months", "[$details]")
            api.route("/v1/families/7", """[{"fingerprint":"$FP_A","nickname":"a$p"}]""")
            api.route("/v1/autonomous-systems/2024-03", """[{"number":1,"name":"AS$p"}]""")
            api.route("/v1/statistics/daily-counts", """[{"day":"2024-03-05$p","total":1}]""")
            api.route("/v1/country-usage/2024-03-05", """[{"country":"d$p","users":1}]""")
            api.route("/v1/country-usage/2024-03-04", """{"message":"bad$p day"}""", status = 400)

            val commands = listOf(
                listOf("days"), listOf("relays"), listOf("relays", "--flag", "exit"), listOf("relay", FP_A),
                listOf("relay", FP_A, "--history"), listOf("relay", FP_A, "--require-flag", "guard"),
                listOf("relay", nickname), listOf("relay", nickname.uppercase()), listOf("family", "7"), listOf("family", FP_A),
                listOf("as"), listOf("stats"), listOf("usage"), listOf("usage", "--day", "2024-03-04"),
                listOf("relays", "--country", "\u001b[2J"), listOf("nonsense\u001b[2J"), listOf("relays", "--help"),
                listOf("completion", "bash"), listOf("completion", "zsh"), listOf("completion", "fish"),
            )
            val exitCodes = mutableSetOf<Int>()
            commands.forEach { command ->
                listOf("table", "json", "ndjson", "csv").forEach { format ->
                    api.run(*command.toTypedArray(), format = format).let { result ->
                        exitCodes += result.exitCode
                        withClue(command, format, "stdout") { forbiddenIn(result.stdout).shouldBeEmpty() }
                        withClue(command, format, "stderr") { forbiddenIn(result.stderr).shouldBeEmpty() }
                    }
                }
            }
            // The poisoned values were printed, and errors were reported, not only rejected early
            exitCodes shouldContainAll setOf(ExitCode.OK, ExitCode.ERROR, ExitCode.AMBIGUOUS, ExitCode.CHECK_FAILED, ExitCode.USAGE)
            api.run("relays", format = "json").stdout shouldContain "\\u202e"
            // JSON keeps the values exactly, as escapes
            Json.parseToJsonElement(api.run("relays", format = "json").stdout).jsonArray[0].jsonObject["nickname"]!!
                .jsonPrimitive.content shouldBe nickname
        }
    }
})

private inline fun withClue(command: List<String>, format: String, stream: String, block: () -> Unit) =
    io.kotest.assertions.withClue("${command.joinToString(" ")} --format $format ($stream)", block)

private infix fun Set<Int>.shouldContainAll(expected: Set<Int>) = (expected - this) shouldBe emptySet()
