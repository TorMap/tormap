package org.tormap.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class UserStatsCsvParserTest : StringSpec({
    fun parse(vararg lines: String): Pair<List<String>, Int> {
        var malformed = 0
        val rows = UserStatsCsvParser.parse(lines.asSequence()) { malformed++ }
            .map { "${it.day} ${it.countryCode} ${it.users} ${it.lower} ${it.upper} ${it.frac}" }
            .toList()
        return rows to malformed
    }

    "parses countries with and without bounds" {
        val (rows, malformed) = parse(
            "2025-01-01,de,1000,800,1200,75",
            "2011-03-06,at,69,,,11",
        )
        rows shouldBe listOf("2025-01-01 DE 1000 800 1200 75", "2011-03-06 AT 69 null null 11")
        malformed shouldBe 0
    }

    "skips comments, the header, the global total and non countries silently" {
        val (rows, malformed) = parse(
            "#",
            "# The Tor Project",
            "date,country,users,lower,upper,frac",
            "",
            "2025-01-01,,1946637,,,75",
            "2025-01-01,??,4327,,,75",
            "2011-03-06,a1,1441,,,11",
            "2011-03-06,a2,422,,,11",
            "2025-01-01,de,10,5,15,75",
        )
        rows shouldBe listOf("2025-01-01 DE 10 5 15 75")
        malformed shouldBe 0
    }

    "counts malformed lines and keeps the valid ones" {
        val (rows, malformed) = parse(
            "2025-01-01,de,abc,,,75",
            "2025-13-45,de,10,,,75",
            "2025-01-01,de,10,,",
            "2025-01-01,de,-5,,,75",
            "2025-01-01,de,10,,,101",
            "2025-01-01,<script>,10,,,75",
            "2025-01-01,de,10,x,,75",
            "2025-01-02,fr,20,,,75",
        )
        rows shouldBe listOf("2025-01-02 FR 20 null null 75")
        malformed shouldBe 7
    }

    "drops negative bounds" {
        UserStatsCsvParser.parse(sequenceOf("2025-01-01,de,10,-1,15,75")).single().lower shouldBe null
    }

    "parses the date strictly" {
        UserStatsCsvParser.parse(sequenceOf("2025-01-01,de,10,,,75")).single().day shouldBe LocalDate.of(2025, 1, 1)
    }
})
