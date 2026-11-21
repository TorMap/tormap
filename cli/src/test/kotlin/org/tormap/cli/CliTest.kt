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

    "usage without a day uses the latest day that has estimates" {
        api.route("/v1/country-usage/2024-03-05", "[]")
        api.route("/v1/country-usage/2024-03-04", "[]")
        api.route("/v1/country-usage/2024-03-03", """[{"country":"de","users":200,"lower":180,"upper":220,"frac":90}]""")
        api.run("usage", format = "csv").stdout.lines()[1] shouldBe "de,200,180,220,90"
        api.requests.filter { it.startsWith("/v1/country-usage/") } shouldContainExactly
            listOf("/v1/country-usage/2024-03-05", "/v1/country-usage/2024-03-04", "/v1/country-usage/2024-03-03")
        api.run("usage", "--day", "2024-03-05").exitCode shouldBe ExitCode.NOT_FOUND
    }

    "usage names the day when it falls back to an earlier one" {
        api.route("/v1/country-usage/2024-03-05", "[]")
        api.route("/v1/country-usage/2024-03-04", """[{"country":"de","users":200,"lower":180,"upper":220,"frac":90}]""")
        api.run("usage").stderr shouldContain "Estimates for 2024-03-04"
        api.run("usage", "--quiet").stderr shouldBe ""
    }

    "usage without estimates in the last week is exit code 3" {
        (0L..7L).forEach { api.route("/v1/country-usage/${java.time.LocalDate.parse("2024-03-05").minusDays(it)}", "[]") }
        api.run("usage").exitCode shouldBe ExitCode.NOT_FOUND
    }

    "--quiet prints nothing, the exit code still tells the result" {
        api.run("relays", "--quiet").let {
            it.exitCode shouldBe ExitCode.OK
            it.stdout shouldBe ""
        }
        api.run("-q", "relays", "--country", "FR").exitCode shouldBe ExitCode.NOT_FOUND
    }

    "--require-flag checks the relay's flags on the day" {
        api.route("/v1/relays/$FP_B?month=2024-03", details(FP_B, "beta"))
        api.run("relay", "beta", "--require-flag", "running,Guard", "-q").exitCode shouldBe ExitCode.OK
        api.run("relay", "beta", "--require-flag", "Exit").let {
            it.exitCode shouldBe ExitCode.CHECK_FAILED
            it.stderr shouldContain "beta ($FP_B) lacks Exit on 2024-03-05"
            it.stdout shouldContain FP_B
        }
        // Details exist for the month, but the relay is not in the day's consensus
        api.route("/v1/relays/$FP_C", details(FP_C, "gone"))
        api.route("/v1/days/2024-03-05/relays", RELAYS_OF_DAY.replace(FP_C, FP_A.replace('A', 'D')))
        api.run("--no-cache", "relay", FP_C, "--require-flag", "Running").let {
            it.exitCode shouldBe ExitCode.NOT_FOUND
            it.stderr shouldContain "is not in the consensus on 2024-03-05"
        }
    }

    "--history lists the months of a relay, by fingerprint or nickname" {
        api.withDay()
        api.route(
            "/v1/relays/$FP_B/months",
            """[{"month":"2024-01","day":"2024-01-20","nickname":"oldbeta","address":"192.0.2.9","autonomousSystemNumber":1101,
               "autonomousSystemName":"SURF","platform":"Tor 0.4.7","contact":null,"bandwidthObserved":100,"familyId":null},
              {"month":"2024-03","day":"2024-03-05","nickname":"beta","address":"192.0.2.1","autonomousSystemNumber":1101,
               "autonomousSystemName":"SURF","platform":"Tor 0.4.8","contact":"x","bandwidthObserved":200,"familyId":7}]""",
        )
        api.run("relay", FP_B, "--history", "--format", "csv").let {
            it.exitCode shouldBe ExitCode.OK
            it.stdout.lines().first() shouldBe Fields.RELAY_MONTH.joinToString(",")
            it.stdout shouldContain "2024-01,2024-01-20,oldbeta,192.0.2.9"
        }
        // A fingerprint needs no day, a nickname is looked up on the latest day
        api.requests shouldBe listOf("/v1/relays/$FP_B/months")
        api.run("relay", "beta", "--history", format = "table").let {
            it.exitCode shouldBe ExitCode.OK
            it.stdout shouldContain "oldbeta"
            it.stdout shouldContain "Tor 0.4.8"
        }
        api.run("relay", FP_A, "--history").exitCode shouldBe ExitCode.NOT_FOUND
        api.run("relay", FP_B, "--history", "--month", "2024-03").exitCode shouldBe ExitCode.USAGE
    }

    "shell completion scripts" {
        listOf("bash", "zsh", "fish").forEach { shell ->
            api.run("completion", shell).let {
                it.exitCode shouldBe ExitCode.OK
                it.stdout shouldContain "relays"
            }
        }
        api.requests shouldBe emptyList()
    }

    "--from after --to is wrong usage, checked before any request" {
        api.run("stats", "--from", "2024-03-05", "--to", "2024-03-04").let {
            it.exitCode shouldBe ExitCode.USAGE
            it.stderr shouldContain "--from 2024-03-05 is after --to 2024-03-04"
        }
        api.run("days", "--from", "2024-03-05", "--to", "2024-03-04").exitCode shouldBe ExitCode.USAGE
        api.requests shouldBe emptyList()
    }

    "a format name as --output file is wrong usage" {
        api.run("-o", "json", "days").let {
            it.exitCode shouldBe ExitCode.USAGE
            it.stderr shouldContain "-f json"
        }
        api.requests shouldBe emptyList()
        java.nio.file.Files.exists(java.nio.file.Path.of("json")) shouldBe false
    }

    "an unreachable proxy is named in the error" {
        val err = java.io.ByteArrayOutputStream()
        val code = runCli(
            arrayOf("--api-url", api.url, "--proxy", "socks5h://127.0.0.1:1", "--no-cache", "days"),
            System.out, java.io.PrintStream(err, true, Charsets.UTF_8),
        ) { null }
        code shouldBe ExitCode.ERROR
        err.toString(Charsets.UTF_8) shouldContain "through the proxy 127.0.0.1:1"
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

    "control characters in ambiguous matches and server error messages do not reach the terminal" {
        val esc = "\\u001b]0;pwned\\u0007"
        api.route(
            "/v1/days/2024-03-05/relays",
            RELAYS_OF_DAY.replace("\"Alphabet\"", "\"alpha\"").replace("\"country\":\"NL\"", "\"country\":\"N$esc\""),
        )
        api.run("relay", "alpha").also { it.exitCode shouldBe ExitCode.AMBIGUOUS }.stderr shouldNotContain "\u001b"
        api.route("/v1/country-usage/2024-03-05", """{"message":"bad$esc day"}""", status = 400)
        api.run("usage", "--day", "2024-03-05").also { it.exitCode shouldBe ExitCode.ERROR }.stderr shouldNotContain "\u001b"
    }

    "invalid days or fingerprints from the server are an error, not a crash" {
        api.route("/v1/days", """["2024-03-04","20\u001b[2J"]""")
        api.run("--no-cache", "days", format = "table").let {
            it.exitCode shouldBe ExitCode.ERROR
            it.stderr shouldContain "invalid day"
            it.stdout shouldNotContain "\u001b"
        }
        api.route("/v1/days", """["2024-03-04",{"day":1}]""")
        api.run("--no-cache", "relays").stderr shouldContain "invalid day"
        api.route("/v1/days", """["2024-03-04","2024-03-05"]""")
        api.route("/v1/days/2024-03-05/relays", RELAYS_OF_DAY.replace(FP_B, "../../x"))
        api.run("--no-cache", "relay", "beta").let {
            it.exitCode shouldBe ExitCode.ERROR
            it.stderr shouldContain "invalid fingerprint"
        }
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
