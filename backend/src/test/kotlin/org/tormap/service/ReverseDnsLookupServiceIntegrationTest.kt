package org.tormap.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.tormap.config.CacheConfig
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@ActiveProfiles("test")
@Import(ReverseDnsLookupServiceIntegrationTestConfig::class)
class ReverseDnsLookupServiceIntegrationTest(
    private val reverseDnsLookupService: ReverseDnsLookupService,
    private val cacheManager: CacheManager,
    private val controllableResolver: ControllableReverseDnsResolver,
) : StringSpec({
    beforeEach {
        cacheManager.getCache(CacheConfig.REVERSE_DNS_LOOKUPS)?.clear()
        controllableResolver.reset()
    }

    "lookupHostNames result is cached so subsequent calls for the same IP skip the resolver" {
        controllableResolver.configure(
            ptrRecords = listOf("host1.example."),
            addresses = mapOf("host1.example" to listOf("1.2.3.4")),
        )

        reverseDnsLookupService.lookupHostNames("1.2.3.4")
        reverseDnsLookupService.lookupHostNames("1.2.3.4")

        controllableResolver.lookupPtrRecordsCallCount.get() shouldBe 1
    }

    "concurrent cache misses for the same IP trigger only one DNS lookup" {
        val threadCount = 10
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)
        controllableResolver.configure(
            ptrRecords = listOf("host1.example."),
            addresses = mapOf("host1.example" to listOf("1.2.3.4")),
            delayMs = 50,
        )

        val executor = Executors.newFixedThreadPool(threadCount)
        try {
            repeat(threadCount) {
                executor.submit {
                    readyLatch.countDown()
                    startLatch.await()
                    reverseDnsLookupService.lookupHostNames("1.2.3.4")
                }
            }
            readyLatch.await()
            startLatch.countDown()
            executor.shutdown()
            executor.awaitTermination(10, TimeUnit.SECONDS)
        } finally {
            executor.shutdownNow()
        }

        controllableResolver.lookupPtrRecordsCallCount.get() shouldBe 1
    }

    "lookupHostNames rejects lookups when all slots are busy and does not cache the rejection" {
        val busyLookupCount = 8
        controllableResolver.configure(
            ptrRecords = listOf("host1.example."),
            addresses = mapOf("host1.example" to listOf("1.2.3.4")),
            delayMs = 1_000,
        )

        val executor = Executors.newFixedThreadPool(busyLookupCount)
        try {
            repeat(busyLookupCount) {
                executor.submit { reverseDnsLookupService.lookupHostNames("10.0.0.${it + 1}") }
            }
            while (controllableResolver.lookupPtrRecordsCallCount.get() < busyLookupCount) Thread.sleep(10)

            shouldThrow<ReverseDnsLookupCapacityException> {
                reverseDnsLookupService.lookupHostNames("1.2.3.4")
            }

            executor.shutdown()
            executor.awaitTermination(10, TimeUnit.SECONDS)
        } finally {
            executor.shutdownNow()
        }

        controllableResolver.configure(
            ptrRecords = listOf("host1.example."),
            addresses = mapOf("host1.example" to listOf("1.2.3.4")),
        )
        reverseDnsLookupService.lookupHostNames("1.2.3.4").verifiedHostNames shouldBe listOf("host1.example")
        controllableResolver.lookupPtrRecordsCallCount.get() shouldBe busyLookupCount + 1
    }
})

class ControllableReverseDnsResolver : ReverseDnsResolver {
    @Volatile
    private var ptrRecords: List<String> = emptyList()
    @Volatile
    private var addresses: Map<String, List<String>> = emptyMap()
    @Volatile
    private var delayMs: Long = 0

    val lookupPtrRecordsCallCount = AtomicInteger(0)

    fun configure(ptrRecords: List<String>, addresses: Map<String, List<String>>, delayMs: Long = 0) {
        this.ptrRecords = ptrRecords
        this.addresses = addresses
        this.delayMs = delayMs
    }

    fun reset() {
        ptrRecords = emptyList()
        addresses = emptyMap()
        delayMs = 0
        lookupPtrRecordsCallCount.set(0)
    }

    override fun lookupPtrRecords(ipAddress: String): List<String> {
        lookupPtrRecordsCallCount.incrementAndGet()
        if (delayMs > 0) Thread.sleep(delayMs)
        return ptrRecords
    }

    override fun lookupAddresses(hostName: String): List<String> = addresses[hostName] ?: emptyList()
}

@TestConfiguration
class ReverseDnsLookupServiceIntegrationTestConfig {
    @Bean
    @Primary
    fun controllableReverseDnsResolver(): ControllableReverseDnsResolver = ControllableReverseDnsResolver()
}
