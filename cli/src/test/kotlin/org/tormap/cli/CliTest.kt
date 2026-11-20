package org.tormap.cli

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CliTest : StringSpec({
    lateinit var api: FakeApi
    beforeEach { api = FakeApi().withDay() }
    afterEach { api.close() }

    fun fingerprints(stdout: String) = Json.parseToJsonElement(stdout).jsonArray.map { it.jsonObject["fingerprint"]!!.jsonPrimitive.content }

    "relays of the latest day, sorted by consensus weight" {
        val result = api.run("relays")
        result.exitCode shouldBe ExitCode.OK
        fingerprints(result.stdout) shouldContainExactly listOf(FP_B, FP_A, FP_C)
        api.requests shouldContainExactly listOf("/v1/days", "/v1/days/2024-03-05/relays")
    }

    "JSON output keeps fields this CLI version does not know" {
        api.run("relays", "--country", "de").stdout shouldContain "\"futureField\": \"kept\""
    }

    "relays are filtered by country, flags, autonomous system, family, nickname and weight" {
        fingerprints(api.run("relays", "-c", "de,nl", "--flag", "guard,exit").stdout) shouldContainExactly listOf(FP_A)
        fingerprints(api.run("relays", "--as", "24940", "--sort", "nickname").stdout) shouldContainExactly listOf(FP_A, FP_C)
        fingerprints(api.run("relays", "--family", "7", "--limit", "1").stdout) shouldContainExactly listOf(FP_B)
        fingerprints(api.run("relays", "--nickname", "ALPHA*", "--sort", "fingerprint").stdout) shouldContainExactly listOf(FP_A, FP_C)
        fingerprints(api.run("relays", "--min-weight", "3000").stdout) shouldContainExactly listOf(FP_B, FP_A)
    }

    "no matching relays is exit code 3" {
        val result = api.run("relays", "--country", "FR")
        result.exitCode shouldBe ExitCode.NOT_FOUND
        result.stdout.trim() shouldBe "[]"
    }

    "a table shows the main columns with numbers right-aligned" {
        val lines = api.run("relays", format = "table").stdout.lines()
        lines[0] shouldBe "fingerprint                               nickname  country  consensusWeight  autonomousSystemNumber  flags"
        lines[1] shouldStartWith "$FP_B  beta      NL                  5000                    1101  Fast Guard Running"
    }

    "CSV has all fields, quotes where needed and defuses spreadsheet formulas" {
        val csv = api.run("relays", "--limit", "1", format = "csv").stdout.lines()
        csv[0] shouldBe Fields.RELAY.joinToString(",")
        csv[1] shouldBe "$FP_B,beta,NL,Fast Guard Running,5000,1101,7,52.3,4.9"
        api.route("/v1/relays/$FP_A?month=2024-03", details(FP_A, "alpha"))
        api.run("relay", FP_A, "--month", "2024-03", format = "csv").stdout shouldContain ",\"'=cmd|calc\","
    }

    "NDJSON prints one relay per line" {
        api.run("relays", format = "ndjson").stdout.trim().lines().size shouldBe 3
    }

    "a relay by fingerprint, also with spaces and a leading dollar sign" {
        api.route("/v1/relays/$FP_A", details(FP_A, "alpha"))
        val spaced = "$" + FP_A.lowercase().chunked(4).joinToString(" ")
        val result = api.run("relay", spaced, format = "table")
        result.exitCode shouldBe ExitCode.OK
        result.stdout shouldContain "nickname                 alpha"
        result.stdout shouldContain "confirmedFamilyMembers   $FP_B"
        api.requests shouldContainExactly listOf("/v1/relays/$FP_A")
    }

    "an unknown fingerprint is exit code 3" {
        val result = api.run("relay", FP_C, "--month", "2024-02")
        result.exitCode shouldBe ExitCode.NOT_FOUND
        result.stderr shouldContain "No relay with fingerprint $FP_C in 2024-02"
    }

    "a relay by nickname, case-insensitive, from the latest day's month" {
        api.route("/v1/relays/$FP_B?month=2024-03", details(FP_B, "beta"))
        val result = api.run("relay", "BETA")
        result.exitCode shouldBe ExitCode.OK
        Json.parseToJsonElement(result.stdout).jsonObject["fingerprint"]!!.jsonPrimitive.content shouldBe FP_B
    }

    "a nickname used by several relays lists their fingerprints with exit code 4" {
        api.route("/v1/days/2024-03-05/relays", RELAYS_OF_DAY.replace("\"Alphabet\"", "\"alpha\""))
        val result = api.run("relay", "alpha")
        result.exitCode shouldBe ExitCode.AMBIGUOUS
        result.stderr shouldContain FP_A
        result.stderr shouldContain FP_C
    }

    "a family by ID or by a member's fingerprint" {
        api.route("/v1/families/7", """[{"fingerprint":"$FP_A","nickname":"alpha"},{"fingerprint":"$FP_B","nickname":"beta"}]""")
        api.route("/v1/relays/$FP_A", details(FP_A, "alpha"))
        api.run("family", "7", format = "csv").stdout.lines().take(3) shouldContainExactly listOf("fingerprint,nickname", "$FP_A,alpha", "$FP_B,beta")
        api.run("family", FP_A, format = "csv").stdout.lines()[1] shouldBe "$FP_A,alpha"
    }

    "days, autonomous systems, statistics and usage" {
        api.run("days", "--latest", format = "table").stdout.trim() shouldBe "2024-03-05"
        api.run("days", "--to", "2024-03-04").stdout shouldContain "2024-03-04"

        api.route("/v1/autonomous-systems/2024-03", """[{"number":24940,"name":"Hetzner Online GmbH"},{"number":1101,"name":"SURF"}]""")
        api.run("as", "--name", "hetzner", format = "csv").stdout.lines()[1] shouldBe "24940,Hetzner Online GmbH"

        api.route("/v1/statistics/daily-counts?from=2024-03-04&to=2024-03-05", """[{"day":"2024-03-04","total":3,"exit":1,"guard":1,"other":1}]""")
        api.run("stats", "--from", "2024-03-04", "--to", "2024-03-05", format = "csv").stdout.lines()[1] shouldBe "2024-03-04,3,1,1,1"

        api.route("/v1/country-usage/2024-03-05", """[{"country":"de","users":200,"lower":null,"upper":null,"frac":90},{"country":"us","users":900,"lower":800,"upper":1000,"frac":90}]""")
        api.run("usage", format = "csv").stdout.lines().drop(1).first() shouldBe "us,900,800,1000,90"
        api.run("usage", "-c", "DE", format = "csv").stdout.lines().drop(1).first() shouldBe "de,200,,,90"
    }

    "the relays of a day are cached between runs, unless --no-cache" {
        api.run("relays").exitCode shouldBe ExitCode.OK
        api.run("relays", "--country", "NL").exitCode shouldBe ExitCode.OK
        api.requests.count { it == "/v1/days/2024-03-05/relays" } shouldBe 1
        api.run("--no-cache", "relays").exitCode shouldBe ExitCode.OK
        api.requests.count { it == "/v1/days/2024-03-05/relays" } shouldBe 2
    }

    "requests identify the CLI version" {
        api.run("days")
        api.userAgents.first() shouldBe "tormap-cli/$VERSION"
    }

    "a malformed day or month is wrong usage, checked before any request" {
        api.run("relays", "--day", "2024-02-30").exitCode shouldBe ExitCode.USAGE
        api.run("relay", FP_A, "--month", "2024-5").exitCode shouldBe ExitCode.USAGE
        api.run("nonsense").exitCode shouldBe ExitCode.USAGE
        api.requests shouldBe emptyList()
    }

    "server errors and redirects are reported as errors" {
        api.route("/v1/days", "oops", status = 500)
        api.run("--no-cache", "days").let {
            it.exitCode shouldBe ExitCode.ERROR
            it.stderr shouldContain "HTTP 500"
        }
        api.route("/v1/days", "", status = 302)
        api.run("--no-cache", "days").exitCode shouldBe ExitCode.ERROR
        api.route("/v1/days", "not json")
        api.run("--no-cache", "days").stderr shouldContain "invalid JSON"
    }

    "an unreachable server is an error" {
        val result = runCli(arrayOf("--api-url", "http://127.0.0.1:1", "--no-cache", "days"), System.out, java.io.PrintStream(java.io.ByteArrayOutputStream())) { null }
        result shouldBe ExitCode.ERROR
    }

    "control characters from relay data do not reach the terminal" {
        api.route("/v1/days/2024-03-05/relays", RELAYS_OF_DAY.replace("\"beta\"", "\"be\\u001b[2Jta\""))
        api.run("relays", format = "table").stdout shouldNotContain "\u001b"
    }

    "--version and --help" {
        api.run("--version").stdout shouldContain VERSION
        api.run("relays", "--help").let {
            it.exitCode shouldBe ExitCode.OK
            it.stdout shouldContain "--country"
        }
    }

    "--output writes to a file" {
        val file = api.cacheDir.resolve("out.csv")
        api.run("relays", "--output", file.toString(), format = "csv").stdout shouldBe ""
        java.nio.file.Files.readString(file) shouldStartWith "fingerprint,"
    }
})
