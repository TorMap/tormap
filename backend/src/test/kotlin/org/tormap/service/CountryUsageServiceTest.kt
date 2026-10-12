package org.tormap.service

import com.sun.net.httpserver.HttpServer
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.tormap.config.CacheConfig
import org.tormap.config.value.UsageStatisticsConfig
import org.tormap.database.repository.CountryUsageRepository
import java.net.InetSocketAddress
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArrayList

@SpringBootTest
@ActiveProfiles("test")
class CountryUsageServiceTest(
    private val countryUsageRepository: CountryUsageRepository,
    private val cacheManager: CacheManager,
    private val jdbcTemplate: JdbcTemplate,
) : StringSpec({
    val requestedUris = CopyOnWriteArrayList<String>()
    var status = 200
    var body = ""
    var redirectTo: String? = null

    // A local stand-in for Tor Metrics
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/") { exchange ->
            requestedUris.add(exchange.requestURI.toString())
            val target = redirectTo
            if (target != null) {
                exchange.responseHeaders.add("Location", target)
                exchange.sendResponseHeaders(302, -1)
            } else {
                val bytes = body.toByteArray()
                exchange.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
                if (bytes.isNotEmpty()) exchange.responseBody.write(bytes)
            }
            exchange.close()
        }
        start()
    }
    val baseUrl = "http://127.0.0.1:${server.address.port}"

    fun service(maxDownloadBytes: Long = 10_000_000) = CountryUsageService(
        UsageStatisticsConfig(
            baseUrl = baseUrl,
            relayUsersPath = "/userstats-relay-country.csv",
            maxDownloadBytes = maxDownloadBytes,
            connectTimeout = Duration.ofSeconds(2),
            requestTimeout = Duration.ofSeconds(5),
            refreshOverlapDays = 14,
        ),
        countryUsageRepository,
        cacheManager,
    )

    beforeEach {
        jdbcTemplate.update("DELETE FROM country_usage")
        cacheManager.getCache(CacheConfig.COUNTRY_USAGE_PER_DAY)?.clear()
        requestedUris.clear()
        status = 200
        redirectTo = null
        body = "#\n# The Tor Project\n#\ndate,country,users,lower,upper,frac\n" +
                "2024-03-04,,300,,,60\n2024-03-04,de,100,80,120,60\n2024-03-04,??,5,,,60\n" +
                "2024-03-05,,310,,,61\n2024-03-05,de,110,90,130,61\n2024-03-05,fr,50,40,60,61\n"
    }

    afterSpec {
        server.stop(0)
    }

    "the first import downloads everything and stores the countries" {
        service().refresh() shouldBe 3
        requestedUris shouldBe listOf("/userstats-relay-country.csv")
        countryUsageRepository.findAllByDay(LocalDate.of(2024, 3, 5)).map { it.country } shouldBe listOf("DE", "FR")
        countryUsageRepository.findAllByDay(LocalDate.of(2024, 3, 4)).map { it.country } shouldBe listOf("DE")
    }

    "later imports only request the days after the latest one, with overlap" {
        service().refresh()
        requestedUris.clear()
        service().refresh()
        requestedUris shouldBe listOf("/userstats-relay-country.csv?start=2024-02-20")
    }

    "an import clears the cache of days" {
        val cache = cacheManager.getCache(CacheConfig.COUNTRY_USAGE_PER_DAY)!!
        cache.put("2024-03-05", listOf<Any>())
        service().refresh()
        cache.get("2024-03-05") shouldBe null
    }

    "a failing response stores nothing" {
        status = 500
        body = ""
        service().refresh() shouldBe 0
        countryUsageRepository.findLatestDay() shouldBe null
    }

    "a response above the size limit is rejected" {
        service(maxDownloadBytes = 50).refresh() shouldBe 0
        countryUsageRepository.findLatestDay() shouldBe null
    }

    "redirects are not followed" {
        redirectTo = "http://127.0.0.1:1/elsewhere.csv"
        service().refresh() shouldBe 0
        requestedUris.size shouldBe 1
    }

    "estimates from the future are ignored" {
        val future = LocalDate.now().plusDays(30)
        body = "date,country,users,lower,upper,frac\n$future,de,1,,,50\n2024-03-05,de,2,,,50\n"
        service().refresh() shouldBe 1
        countryUsageRepository.findLatestDay() shouldBe LocalDate.of(2024, 3, 5)
    }
})
