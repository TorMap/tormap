package org.tormap.cli

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

/** A failure the user should see as one line, e.g. an unreachable server or a malformed response. */
class ApiException(message: String) : Exception(message)

/**
 * Read-only client of the TorMap API v1 (api/openapi-v1.json). Uses only the JDK, so it runs in native images.
 *
 * Responses are parsed into JSON trees rather than classes: fields added by a newer server are kept when the
 * output is JSON, and the CLI reads only the fields listed in [Fields], which the contract tests check against the spec.
 */
class ApiClient(
    baseUrl: String,
    private val proxy: Proxy = Proxy.NO_PROXY,
    private val timeoutMillis: Int = 30_000,
    private val cache: ResponseCache? = null,
) {
    companion object {
        const val DEFAULT_URL = "https://api.tormap.org"

        // A day of relays is about 3 MB of JSON; anything far larger is not a TorMap response
        private const val MAX_RESPONSE_BYTES = 64L * 1024 * 1024
        private val FINGERPRINT = Regex("^[0-9A-F]{40}$")

        /** Parses `socks5h://host:port`, `socks5://host:port` or `http://host:port`. */
        fun parseProxy(value: String): Proxy {
            val uri = try {
                URI(value.trim())
            } catch (_: Exception) {
                null
            }
            val host = uri?.host
            val port = uri?.port ?: -1
            if (uri == null || host == null || port !in 1..65535) {
                throw ApiException("The proxy must look like socks5h://127.0.0.1:9050 or http://proxy:8080.")
            }
            // Unresolved, so the proxy resolves the API host name: DNS requests go through Tor, not around it
            val address = InetSocketAddress.createUnresolved(host, port)
            return when (uri.scheme?.lowercase()) {
                "socks5h", "socks5", "socks" -> Proxy(Proxy.Type.SOCKS, address)
                "http" -> Proxy(Proxy.Type.HTTP, address)
                else -> throw ApiException("Unsupported proxy scheme '${uri.scheme}'. Use socks5h:// or http://.")
            }
        }
    }

    private val baseUri: URI = try {
        URI(baseUrl.trim().trimEnd('/') + "/")
    } catch (_: Exception) {
        throw ApiException("The API URL '$baseUrl' is not a valid URL.")
    }.also {
        if (it.scheme?.lowercase() !in setOf("http", "https") || it.host == null) {
            throw ApiException("The API URL must start with https:// or http://.")
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    fun days(): List<String> = (getCached("v1/days", ResponseCache.SHORT) as JsonArray).map { it.jsonPrimitive.content }

    fun latestDay(): String = days().lastOrNull() ?: throw ApiException("The API has no relay data yet.")

    fun relaysOfDay(day: String): JsonArray =
        getCached("v1/days/${encode(day)}/relays", ResponseCache.ttlForDay(day)).jsonArray

    /** The relay details of a month, or null if the relay was not seen in that month. */
    fun relay(fingerprint: String, month: String?): JsonObject? {
        require(FINGERPRINT.matches(fingerprint)) { "Not a fingerprint: $fingerprint" }
        val query = month?.let { "?month=${encode(it)}" }.orEmpty()
        return get("v1/relays/$fingerprint$query")?.jsonObject
    }

    fun family(id: Long): JsonArray = get("v1/families/$id")?.jsonArray ?: JsonArray(emptyList())

    fun autonomousSystems(month: String): JsonArray = required("v1/autonomous-systems/${encode(month)}").jsonArray

    fun dailyCounts(from: String?, to: String?): JsonArray {
        val query = listOfNotNull(from?.let { "from=${encode(it)}" }, to?.let { "to=${encode(it)}" }).joinToString("&")
        return required("v1/statistics/daily-counts" + if (query.isEmpty()) "" else "?$query").jsonArray
    }

    fun countryUsage(day: String): JsonArray = required("v1/country-usage/${encode(day)}").jsonArray

    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun getCached(path: String, ttl: java.time.Duration): JsonElement {
        val key = baseUri.resolve(path).toString()
        cache?.read(key, ttl)?.let { return parse(it, path) }
        val body = request(path) ?: throw notFound(path)
        val element = parse(body, path)
        // Empty lists are not cached: the day may get data later
        if (!(element is JsonArray && element.isEmpty())) cache?.write(key, body)
        return element
    }

    private fun get(path: String): JsonElement? = request(path)?.let { parse(it, path) }

    private fun required(path: String): JsonElement = get(path) ?: throw notFound(path)

    private fun notFound(path: String) = ApiException("The API returned 404 for /$path. Does ${baseUri.host} run TorMap 4.3 or newer?")

    private fun parse(body: String, path: String): JsonElement = try {
        json.parseToJsonElement(body)
    } catch (_: Exception) {
        throw ApiException("The API returned invalid JSON for /$path.")
    }

    /** Returns the body, or null for 404. */
    private fun request(path: String): String? {
        val uri = baseUri.resolve(path)
        val connection = try {
            uri.toURL().openConnection(proxy) as HttpURLConnection
        } catch (exception: IOException) {
            throw ApiException("Can not connect to ${baseUri.host}: ${exception.message}")
        }
        try {
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            // A redirect could lead to another host or from https to http, so it is an error instead
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Accept-Encoding", "gzip")
            connection.setRequestProperty("User-Agent", "tormap-cli/$VERSION")
            val status = try {
                connection.responseCode
            } catch (exception: IOException) {
                throw ApiException("Can not reach ${baseUri.host}: ${exception.message ?: exception.javaClass.simpleName}")
            }
            return when {
                status == 200 -> readBody(connection, connection.inputStream)
                status == 404 -> null
                status == 400 -> throw ApiException("The API rejected the request: ${errorMessage(connection)}")
                // Backends without API v1 deny unknown paths
                status == 403 -> throw ApiException("The API returned HTTP 403 for /$path. Does ${baseUri.host} run TorMap 4.3 or newer?")
                else -> throw ApiException("The API returned HTTP $status for /$path.")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun errorMessage(connection: HttpURLConnection): String {
        val body = connection.errorStream?.let { stream -> runCatching { readBody(connection, stream) }.getOrNull() }
        val message = body?.let { runCatching { json.parseToJsonElement(it).jsonObject["message"]?.jsonPrimitive?.content }.getOrNull() }
        return message?.takeIf { it.isNotBlank() } ?: "bad request"
    }

    private fun readBody(connection: HttpURLConnection, stream: InputStream): String {
        val decoded = if (connection.contentEncoding.equals("gzip", ignoreCase = true)) GZIPInputStream(stream) else stream
        decoded.use { input ->
            val bytes = input.readNBytes((MAX_RESPONSE_BYTES + 1).toInt())
            if (bytes.size > MAX_RESPONSE_BYTES) throw ApiException("The API response is larger than ${MAX_RESPONSE_BYTES / 1024 / 1024} MB.")
            return String(bytes, StandardCharsets.UTF_8)
        }
    }
}
