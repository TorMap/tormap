package org.tormap.cli

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.net.InetSocketAddress
import java.net.Proxy
import java.time.Clock
import java.time.Duration
import java.time.LocalDate

class ApiClientTest :
    StringSpec({
        "a SOCKS proxy leaves name resolution to the proxy" {
            listOf("socks5h://127.0.0.1:9050", "socks5://localhost:9150").forEach { value ->
                val proxy = ApiClient.parseProxy(value)
                proxy.type() shouldBe Proxy.Type.SOCKS
                (proxy.address() as InetSocketAddress).isUnresolved shouldBe true
            }
            ApiClient.parseProxy("http://proxy:8080").type() shouldBe Proxy.Type.HTTP
        }

        "malformed proxies and API URLs are rejected" {
            listOf("127.0.0.1:9050", "socks5h://127.0.0.1", "ftp://proxy:21", "socks5h://:9050").forEach { value ->
                shouldThrow<ApiException> { ApiClient.parseProxy(value) }
            }
            listOf("api.tormap.org", "ftp://api.tormap.org", "file:///etc/passwd").forEach { value ->
                shouldThrow<ApiException> { ApiClient(value) }
            }
        }

        "days of the current and previous month are refreshed after an hour, older days are kept longer" {
            val today = LocalDate.of(2024, 3, 10)
            ResponseCache.ttlForDay("2024-03-09", today) shouldBe Duration.ofHours(1)
            // The server reprocesses whole months, so early days of a recent month still change
            ResponseCache.ttlForDay("2024-03-01", today) shouldBe Duration.ofHours(1)
            ResponseCache.ttlForDay("2024-02-01", today) shouldBe Duration.ofHours(1)
            ResponseCache.ttlForDay("2024-01-31", today) shouldBe Duration.ofDays(30)
            ResponseCache.ttlForDay("2023-12-15", LocalDate.of(2024, 1, 2)) shouldBe Duration.ofHours(1)
            ResponseCache.ttlForDay("2023-11-30", LocalDate.of(2024, 1, 2)) shouldBe Duration.ofDays(30)
        }

        "an expired day is revalidated with its ETag and not downloaded again while it is unchanged" {
            FakeApi().use { api ->
                api.withDay()
                val path = "/v1/days/2024-03-05/relays"
                api.eTags[path] = "W/\"one\""
                fun client(offset: Duration) =
                    ApiClient(api.url, cache = ResponseCache(api.cacheDir, Clock.offset(Clock.systemUTC(), offset)))

                client(Duration.ZERO).relaysOfDay("2024-03-05").size shouldBe 3
                api.ifNoneMatch.last() shouldBe null

                // Older days are kept for 30 days, so 31 days later the entry has expired
                val later = client(Duration.ofDays(31))
                later.relaysOfDay("2024-03-05").size shouldBe 3
                api.ifNoneMatch.last() shouldBe "W/\"one\""
                api.requests.count { it == path } shouldBe 2
                // The 304 made the entry fresh again
                later.relaysOfDay("2024-03-05").size shouldBe 3
                api.requests.count { it == path } shouldBe 2

                // A changed day is downloaded with its new ETag
                api.route(path, "[" + RELAYS_OF_DAY.trim().removePrefix("[").substringBefore("},") + "}]")
                api.eTags[path] = "W/\"two\""
                val muchLater = client(Duration.ofDays(62))
                muchLater.relaysOfDay("2024-03-05").size shouldBe 1
                client(Duration.ofDays(93)).relaysOfDay("2024-03-05").size shouldBe 1
                api.ifNoneMatch.last() shouldBe "W/\"two\""
            }
        }

        "ETags that could not be sent back as a header are not kept" {
            isValidETag("W/\"abc\"") shouldBe true
            isValidETag("a\r\nX-Injected: 1") shouldBe false
            isValidETag("") shouldBe false
            isValidETag("x".repeat(300)) shouldBe false
        }

        "options of the root command may follow the subcommand" {
            val proxy = "--proxy=socks5h://h:1"
            hoistGlobalOptions(
                arrayOf("relays", "--country", "DE", "--format", "csv", "-o", "x.csv", "--no-cache", proxy),
            )
                .toList() shouldBe
                listOf("--format", "csv", "-o", "x.csv", "--no-cache", proxy, "relays", "--country", "DE")
            hoistGlobalOptions(arrayOf("relay", "--", "--format")).toList() shouldBe listOf("relay", "--", "--format")
        }

        "fingerprints are accepted with spaces and a leading dollar sign" {
            normalizeFingerprint("\$aaaa " + "A".repeat(36)) shouldBe "A".repeat(40)
            normalizeFingerprint("alpha") shouldBe null
            normalizeFingerprint("G".repeat(40)) shouldBe null
        }
    })
