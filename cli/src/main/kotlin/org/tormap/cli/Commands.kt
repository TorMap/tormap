package org.tormap.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.YearMonth

/** Exit codes, documented in cli/README.md */
object ExitCode {
    const val OK = 0
    const val ERROR = 1
    const val USAGE = 2
    const val NOT_FOUND = 3
    const val AMBIGUOUS = 4
    const val CHECK_FAILED = 5
}

/** What the root command parsed, shared with the subcommands */
class Settings(
    val api: ApiClient,
    val format: Format,
    private val stdout: PrintStream,
    private val stderr: PrintStream,
    private val outputFile: Path?,
    private val quiet: Boolean,
) {
    /** Writes the results to the terminal or to --output. With --quiet nothing reaches the terminal. */
    fun output(block: (Output) -> Unit) {
        if (outputFile != null) {
            PrintStream(Files.newOutputStream(outputFile), false, Charsets.UTF_8).use { block(Output(it, format)) }
        } else if (!quiet) {
            block(Output(stdout, format))
        }
    }

    /** A hint about the results on stderr, so it never mixes with JSON or CSV on stdout. Silent with --quiet. */
    fun note(message: String) {
        if (!quiet) stderr.println("tormap: $message")
    }
}

/** A relay was found but lacks flags the user requires (`relay --require-flag`), exit code 5 */
class CheckFailed(message: String) : Exception(message)

private val FORMAT_NAMES = listOf("table", "json", "ndjson", "csv")
private val DAY = Regex("^\\d{4}-\\d{2}-\\d{2}$")
private val MONTH = Regex("^\\d{4}-\\d{2}$")
private val HEX_FINGERPRINT = Regex("^[0-9A-Fa-f]{40}$")

private fun validDay(value: String): String {
    if (!DAY.matches(value) || runCatching { LocalDate.parse(value) }.isFailure) fail("'$value' is not a day like 2024-05-01.")
    return value
}

private fun validMonth(value: String): String {
    if (!MONTH.matches(value) || runCatching { YearMonth.parse(value) }.isFailure) fail("'$value' is not a month like 2024-05.")
    return value
}

private fun fail(message: String): Nothing = throw UsageError(message)

private fun CoreCliktCommand.checkRange(from: String?, to: String?) {
    if (from != null && to != null && from > to) throw UsageError("--from $from is after --to $to.").also { it.context = currentContext }
}

/** Accepts fingerprints as Tor tools print them: with spaces between groups or a leading $. */
fun normalizeFingerprint(value: String): String? =
    value.removePrefix("$").replace(" ", "").takeIf { HEX_FINGERPRINT.matches(it) }?.uppercase()

class NotFound(message: String) : Exception(message)

class Tormap(
    private val stdout: PrintStream,
    private val stderr: PrintStream,
    private val env: (String) -> String?,
) : CoreCliktCommand(name = "tormap") {
    init {
        versionOption(VERSION, names = setOf("--version"))
        context { readEnvvar = env }
    }

    override fun help(context: Context) = "Look up Tor relays and export TorMap data from the command line."

    override fun helpEpilog(context: Context) =
        "Data: TorMap (https://tormap.org), from Tor Project CollecTor and Tor Metrics. IP geolocation and autonomous " +
                "systems by DB-IP (https://db-ip.com), licensed under CC BY 4.0. Exit codes: 0 ok, 1 error, 2 wrong usage, " +
                "3 nothing found, 4 more than one relay matches, 5 a relay lacks a required flag."

    private val apiUrl by option("--api-url", envvar = "TORMAP_API_URL", help = "TorMap backend to use").default(ApiClient.DEFAULT_URL)
    private val proxy by option("--proxy", envvar = "TORMAP_PROXY", help = "Proxy for all requests, e.g. socks5h://127.0.0.1:9050 for Tor")
    private val format by option("--format", "-f", help = "Output format (default: table in a terminal, json otherwise)")
        .choice(*FORMAT_NAMES.toTypedArray())
    private val output by option("--output", "-o", help = "Write the output to a file instead of the terminal").path()
    private val cacheDir by option("--cache-dir", envvar = "TORMAP_CACHE_DIR", help = "Where downloaded days are cached").path()
    private val noCache by option("--no-cache", help = "Download everything again and do not cache it").flag()
    private val quiet by option("--quiet", "-q", help = "Print no results or hints, only errors. Check the exit code instead").flag()
    private val timeout by option("--timeout", help = "Seconds to wait for the server").int().default(30)

    override fun run() {
        val outputName = output?.toString()
        if (outputName != null && outputName.lowercase() in FORMAT_NAMES) {
            fail("-o/--output is the file to write. For the format, use -f $outputName (or -o ./$outputName for a file of that name).")
        }
        val proxy = proxy?.let { ApiClient.parseProxy(it) } ?: java.net.Proxy.NO_PROXY
        val cache = if (noCache) null else ResponseCache(cacheDir ?: ResponseCache.defaultDirectory(env))
        val api = ApiClient(apiUrl, proxy, timeout * 1000, cache)
        val chosen = format?.let { Format.valueOf(it.uppercase()) }
            ?: if (output == null && System.console()?.isTerminal == true) Format.TABLE else Format.JSON
        currentContext.findOrSetObject { Settings(api, chosen, stdout, stderr, output, quiet) }
    }
}

class Days : CoreCliktCommand(name = "days") {
    override fun help(context: Context) = "List the days with relay data."
    private val settings by requireObject<Settings>()
    private val from by option("--from", help = "First day, like 2024-05-01").convertDay()
    private val to by option("--to", help = "Last day").convertDay()
    private val latest by option("--latest", help = "Only print the latest day").flag()

    override fun run() {
        val first = from
        val last = to
        checkRange(first, last)
        val days = settings.api.days().filter { (first == null || it >= first) && (last == null || it <= last) }
        settings.output { it.values("day", if (latest) days.takeLast(1) else days) }
    }
}

class Relays : CoreCliktCommand(name = "relays") {
    override fun help(context: Context) = "List the relays of a day, optionally filtered and sorted."
    private val settings by requireObject<Settings>()
    private val day by option("--day", help = "Day like 2024-05-01 (default: latest)").convertDay()
    private val countries by option("--country", "-c", help = "Country codes, e.g. DE,NL").split(",").default(emptyList())
    private val flags by option("--flag", help = "Flags the relay must all have, e.g. Exit,Guard").split(",").default(emptyList())
    private val autonomousSystems by option("--as", help = "Autonomous system numbers").long().multiple()
    private val family by option("--family", help = "Family ID").long()
    private val nickname by option("--nickname", "-n", help = "Nickname, * and ? match any characters").convert { glob(it) }
    private val minWeight by option("--min-weight", help = "Minimum consensus weight").long()
    private val sort by option("--sort", help = "Sort order (default: weight)").choice("weight", "nickname", "country", "fingerprint").default("weight")
    private val limit by option("--limit", help = "Print at most this many relays").int()

    override fun run() {
        val api = settings.api
        val relays = api.relaysOfDay(day ?: api.latestDay()).map { it.jsonObject }
        val wantedCountries = countries.map { it.trim().uppercase() }.toSet()
        val wantedFlags = flags.map { it.trim().lowercase() }.toSet()
        val pattern = nickname
        val filtered = relays.filter { relay ->
            (wantedCountries.isEmpty() || relay.text("country").uppercase() in wantedCountries) &&
                    (wantedFlags.isEmpty() || relay.text("flags").lowercase().split(" ").toSet().containsAll(wantedFlags)) &&
                    (autonomousSystems.isEmpty() || relay.text("autonomousSystemNumber").toLongOrNull() in autonomousSystems) &&
                    (family == null || relay.text("familyId").toLongOrNull() == family) &&
                    (pattern == null || pattern.matches(relay.text("nickname"))) &&
                    (minWeight == null || (relay.text("consensusWeight").toLongOrNull() ?: -1) >= minWeight!!)
        }
        val sorted = when (sort) {
            "nickname" -> filtered.sortedBy { it.text("nickname").lowercase() }
            "country" -> filtered.sortedWith(compareBy({ it.text("country") }, { -(it.text("consensusWeight").toLongOrNull() ?: -1) }))
            "fingerprint" -> filtered.sortedBy { it.text("fingerprint") }
            else -> filtered.sortedByDescending { it.text("consensusWeight").toLongOrNull() ?: -1 }
        }
        val rows = limit?.let { sorted.take(it) } ?: sorted
        settings.output { it.rows(rows, Fields.RELAY_TABLE, Fields.RELAY) }
        if (rows.isEmpty()) throw ProgramResult(ExitCode.NOT_FOUND)
    }
}

class Relay : CoreCliktCommand(name = "relay") {
    override fun help(context: Context) = "Show the details of one relay, by fingerprint or nickname."
    private val settings by requireObject<Settings>()
    private val query by argument("FINGERPRINT_OR_NICKNAME")
    private val month by option("--month", help = "Month like 2024-05 (default: latest)").convert { validMonth(it) }
    private val day by option("--day", help = "Day for nickname lookups and --require-flag (default: latest)").convertDay()
    private val requiredFlags by option(
        "--require-flag",
        help = "Exit with code 5 unless the relay has all these flags on the day, e.g. Running,Guard. Exit code 3 if it is not in that day's consensus",
    ).split(",").default(emptyList())
    private val history by option(
        "--history",
        help = "List every month the relay published a server descriptor, with its nickname, address, AS, platform and contact then",
    ).flag()

    override fun run() {
        val api = settings.api
        if (history) {
            if (month != null) throw UsageError("--history lists all months, so it can not be combined with --month.")
            val fingerprint = normalizeFingerprint(query) ?: findFingerprint(api, query, day)
            val months = api.relayMonths(fingerprint).map { it.jsonObject }
            if (months.isEmpty()) throw NotFound("No months with details for $fingerprint.")
            settings.output { it.rows(months, Fields.RELAY_MONTH_TABLE, Fields.RELAY_MONTH) }
            if (requiredFlags.isNotEmpty()) checkFlags(api, fingerprint, months.last().text("nickname"))
            return
        }
        val details = findRelay(api, query, month, day)
        settings.output { it.single(details, Fields.RELAY_DETAILS) }
        if (requiredFlags.isNotEmpty()) checkFlags(api, details.text("fingerprint"), details.text("nickname"))
    }

    /** For monitoring, e.g. from cron: the flags come from the day's consensus, the details are per month. */
    private fun checkFlags(api: ApiClient, fingerprint: String, nickname: String) {
        val checkDay = day ?: api.latestDay()
        val relay = api.relaysOfDay(checkDay).map { it.jsonObject }.firstOrNull { it.text("fingerprint") == fingerprint }
            ?: throw NotFound("$nickname ($fingerprint) is not in the consensus on $checkDay.")
        val flags = relay.text("flags").split(" ").filter(String::isNotEmpty)
        val missing = requiredFlags.map(String::trim).filter(String::isNotEmpty)
            .filter { wanted -> flags.none { it.equals(wanted, ignoreCase = true) } }
        if (missing.isNotEmpty()) {
            throw CheckFailed("$nickname ($fingerprint) lacks ${missing.joinToString(", ")} on $checkDay. Its flags: ${flags.joinToString(" ")}.")
        }
    }
}

class Family : CoreCliktCommand(name = "family") {
    override fun help(context: Context) = "List the members of a relay family, by a member's fingerprint or nickname, or by family ID."
    private val settings by requireObject<Settings>()
    private val query by argument("FINGERPRINT_NICKNAME_OR_FAMILY_ID")
    private val month by option("--month", help = "Month like 2024-05 (default: latest)").convert { validMonth(it) }

    override fun run() {
        val api = settings.api
        val familyId = query.toLongOrNull()?.takeIf { query.length < 40 }
            ?: findRelay(api, query, month, null).text("familyId").toLongOrNull()
            ?: throw NotFound("The relay has no confirmed family.")
        val members = api.family(familyId).map { it.jsonObject }
        if (members.isEmpty()) throw NotFound("Family $familyId was not found.")
        settings.output { it.rows(members, Fields.FAMILY_MEMBER) }
    }
}

class AutonomousSystems : CoreCliktCommand(name = "as") {
    override fun help(context: Context) = "List the autonomous systems with relays in a month. Use 'relays --as' for their relays."
    private val settings by requireObject<Settings>()
    private val month by option("--month", help = "Month like 2024-05 (default: latest)").convert { validMonth(it) }
    private val name by option("--name", help = "Only systems whose name contains this text")

    override fun run() {
        val api = settings.api
        val systems = api.autonomousSystems(month ?: api.latestDay().take(7)).map { it.jsonObject }
            .filter { name == null || it.text("name").contains(name!!, ignoreCase = true) }
        settings.output { it.rows(systems, Fields.AUTONOMOUS_SYSTEM) }
        if (systems.isEmpty()) throw ProgramResult(ExitCode.NOT_FOUND)
    }
}

class Stats : CoreCliktCommand(name = "stats") {
    override fun help(context: Context) = "Number of relays per day (total, exit, guard, other)."
    private val settings by requireObject<Settings>()
    private val from by option("--from", help = "First day, like 2024-05-01").convertDay()
    private val to by option("--to", help = "Last day").convertDay()

    override fun run() {
        checkRange(from, to)
        val counts = settings.api.dailyCounts(from, to).map { it.jsonObject }
        settings.output { it.rows(counts, Fields.DAILY_COUNT) }
    }
}

class Usage : CoreCliktCommand(name = "usage") {
    override fun help(context: Context) = "Estimated Tor users per country for a day, from Tor Metrics."
    private val settings by requireObject<Settings>()
    private val day by option("--day", help = "Day like 2024-05-01 (default: the latest day with estimates, usually 2 to 3 days ago)").convertDay()
    private val countries by option("--country", "-c", help = "Country codes, e.g. DE,NL").split(",").default(emptyList())

    override fun run() {
        val api = settings.api
        val wanted = countries.map { it.trim().uppercase() }.toSet()
        val rows = (day?.let { api.countryUsage(it) } ?: latestCountryUsage(api, settings)).map { it.jsonObject }
            .filter { wanted.isEmpty() || it.text("country").uppercase() in wanted }
            .sortedByDescending { it.text("users").toLongOrNull() ?: 0 }
        settings.output { it.rows(rows, Fields.COUNTRY_USAGE) }
        if (rows.isEmpty()) throw ProgramResult(ExitCode.NOT_FOUND)
    }
}

/** Tor Metrics publishes its estimates a few days after the relay data, so the latest relay day usually has none yet. */
private const val USAGE_LOOKBACK_DAYS = 7L

/** The latest day with estimates. Usage rows carry no day, so the day is named in a hint when it is not the latest relay day. */
private fun latestCountryUsage(api: ApiClient, settings: Settings): JsonArray {
    val latestRelayDay = api.latestDay()
    val latest = LocalDate.parse(latestRelayDay)
    val (found, rows) = (0..USAGE_LOOKBACK_DAYS).asSequence()
        .map { latest.minusDays(it).toString() }
        .map { it to api.countryUsage(it) }
        .firstOrNull { it.second.isNotEmpty() }
        ?: throw NotFound("Tor Metrics has no estimates for the $USAGE_LOOKBACK_DAYS days up to $latestRelayDay.")
    if (found != latestRelayDay) {
        settings.note("Estimates for $found, the latest day Tor Metrics has published (relay data goes up to $latestRelayDay).")
    }
    return rows
}

/** Finds a relay by fingerprint, or by its nickname in the relays of a day (case-insensitive, must be unique). */
private fun findRelay(api: ApiClient, query: String, month: String?, day: String?): JsonObject {
    normalizeFingerprint(query)?.let { fingerprint ->
        return api.relay(fingerprint, month) ?: throw NotFound("No relay with fingerprint $fingerprint in ${month ?: "the latest month"}.")
    }
    val lookupDay = day ?: api.latestDay()
    val fingerprint = findFingerprint(api, query, lookupDay)
    return api.relay(fingerprint, month ?: lookupDay.take(7))
        ?: throw NotFound("No details for '$query' in ${month ?: lookupDay.take(7)}.")
}

/** The fingerprint of the relay with this nickname on the day (default: latest) */
private fun findFingerprint(api: ApiClient, nickname: String, day: String?): String {
    val lookupDay = day ?: api.latestDay()
    val matches = api.relaysOfDay(lookupDay).map { it.jsonObject }.filter { it.text("nickname").equals(nickname, ignoreCase = true) }
    return when (matches.size) {
        0 -> throw NotFound("No relay named '$nickname' on $lookupDay.")
        1 -> matches.single().text("fingerprint")
        else -> throw Ambiguous(nickname, lookupDay, matches)
    }
}

class Ambiguous(val query: String, val day: String, val matches: List<JsonObject>) : Exception()

private fun glob(pattern: String): Regex =
    Regex(pattern.split("*").joinToString(".*") { part -> part.split("?").joinToString(".") { Regex.escape(it) } }, RegexOption.IGNORE_CASE)

private fun com.github.ajalt.clikt.parameters.options.NullableOption<String, String>.convertDay() = convert { validDay(it) }
