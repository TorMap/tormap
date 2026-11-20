package org.tormap.cli

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.net.InetSocketAddress
import java.net.Proxy
import java.time.Duration
import java.time.LocalDate

class ApiClientTest : StringSpec({
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

    "recent days are refreshed after an hour, older days are kept longer" {
        val today = LocalDate.of(2024, 3, 10)
        ResponseCache.ttlForDay("2024-03-09", today) shouldBe Duration.ofHours(1)
        ResponseCache.ttlForDay("2024-03-07", today) shouldBe Duration.ofHours(1)
        ResponseCache.ttlForDay("2024-03-01", today) shouldBe Duration.ofDays(30)
    }

    "options of the root command may follow the subcommand" {
        hoistGlobalOptions(arrayOf("relays", "--country", "DE", "--format", "csv", "-o", "x.csv", "--no-cache", "--proxy=socks5h://h:1"))
            .toList() shouldBe listOf("--format", "csv", "-o", "x.csv", "--no-cache", "--proxy=socks5h://h:1", "relays", "--country", "DE")
        hoistGlobalOptions(arrayOf("relay", "--", "--format")).toList() shouldBe listOf("relay", "--", "--format")
    }

    "fingerprints are accepted with spaces and a leading dollar sign" {
        normalizeFingerprint("\$aaaa " + "A".repeat(36)) shouldBe "A".repeat(40)
        normalizeFingerprint("alpha") shouldBe null
        normalizeFingerprint("G".repeat(40)) shouldBe null
    }
})
