package org.tormap.cli

import com.sun.net.httpserver.HttpServer
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.zip.GZIPOutputStream

/** A local stand-in for the TorMap API: serves fixed bodies per path and records the requests. */
class FakeApi : AutoCloseable {
    val routes = mutableMapOf<String, Pair<Int, String>>()
    val requests = CopyOnWriteArrayList<String>()
    val userAgents = CopyOnWriteArrayList<String>()

    /** ETags per path. A request with a matching If-None-Match gets 304 Not Modified. */
    val eTags = mutableMapOf<String, String>()
    val ifNoneMatch = CopyOnWriteArrayList<String?>()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/") { exchange ->
            val target = exchange.requestURI.toString()
            requests += target
            userAgents += exchange.requestHeaders.getFirst("User-Agent").orEmpty()
            ifNoneMatch += exchange.requestHeaders.getFirst("If-None-Match")
            val (status, body) = routes[target] ?: (404 to """{"status":404}""")
            val eTag = eTags[target]
            if (eTag != null) exchange.responseHeaders.add("ETag", eTag)
            if (eTag != null && exchange.requestHeaders.getFirst("If-None-Match") == eTag) {
                exchange.sendResponseHeaders(304, -1)
                exchange.close()
                return@createContext
            }
            val gzip = exchange.requestHeaders.getFirst("Accept-Encoding")?.contains("gzip") == true
            val bytes = if (gzip) {
                ByteArrayOutputStream().also {
                    GZIPOutputStream(it).use { out -> out.write(body.toByteArray()) }
                }.toByteArray()
            } else {
                body.toByteArray()
            }
            if (gzip) exchange.responseHeaders.add("Content-Encoding", "gzip")
            if (status in 300..399) exchange.responseHeaders.add("Location", "http://example.com/")
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        start()
    }
    val url = "http://127.0.0.1:${server.address.port}"
    val cacheDir: Path = Files.createTempDirectory("tormap-cli-test")

    fun route(path: String, body: String, status: Int = 200) {
        routes[path] = status to body
    }

    override fun close() = server.stop(0)
}

class Result(val exitCode: Int, val stdout: String, val stderr: String)

fun FakeApi.run(vararg args: String, format: String = "json"): Result {
    val out = ByteArrayOutputStream()
    val err = ByteArrayOutputStream()
    val all = arrayOf("--api-url", url, "--cache-dir", cacheDir.toString(), "--format", format) + args
    val code = runCli(all, PrintStream(out, true, Charsets.UTF_8), PrintStream(err, true, Charsets.UTF_8)) { null }
    return Result(code, out.toString(Charsets.UTF_8), err.toString(Charsets.UTF_8))
}

const val FP_A = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
const val FP_B = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"
const val FP_C = "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC"

/** Three relays of 2024-03-05 as the API returns them, with a field this CLI version does not know. */
val RELAYS_OF_DAY = """
[
 {"fingerprint":"$FP_A","nickname":"alpha","country":"DE","lat":50.1,"long":8.6,"flags":["Exit","Fast","Guard","Running"],"consensusWeight":3000,"autonomousSystemNumber":24940,"familyId":7,"futureField":"kept"},
 {"fingerprint":"$FP_B","nickname":"beta","country":"NL","lat":52.3,"long":4.9,"flags":["Fast","Guard","Running"],"consensusWeight":5000,"autonomousSystemNumber":1101,"familyId":7},
 {"fingerprint":"$FP_C","nickname":"Alphabet","country":"DE","lat":48.1,"long":11.5,"flags":["Running"],"consensusWeight":10,"autonomousSystemNumber":24940,"familyId":null}
]
""".trimIndent()

fun details(fingerprint: String, nickname: String, month: String = "2024-03") = """
{"fingerprint":"$fingerprint","nickname":"$nickname","month":"$month","day":"$month-05","address":"192.0.2.1",
 "autonomousSystemNumber":24940,"autonomousSystemName":"Hetzner","platform":"Tor 0.4.8","contact":"=cmd|calc",
 "familyId":7,"confirmedFamilyMembers":[{"fingerprint":"$FP_B","nickname":"beta"}],"hibernating":false,
 "verifiedHostNames":["tor.example.org"],"unverifiedHostNames":[]}
""".trimIndent()

fun FakeApi.withDay() = apply {
    route("/v1/days", """["2024-03-04","2024-03-05"]""")
    route("/v1/days/2024-03-05/relays", RELAYS_OF_DAY)
}
