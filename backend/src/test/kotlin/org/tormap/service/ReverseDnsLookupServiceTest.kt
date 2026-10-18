package org.tormap.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class ReverseDnsLookupServiceTest : StringSpec({
    "lookupHostNames separates verified and unverified host names" {
        val reverseDnsResolver = LookupServiceFakeReverseDnsResolver(
            ptrRecordsByIpAddress = mapOf(
                "1.2.3.4" to listOf(
                    "verified.example.",
                    "verified.example.",
                    "unverified.example.",
                )
            ),
            addressesByHostName = mapOf(
                "verified.example" to listOf("1.2.3.4"),
                "unverified.example" to listOf("5.6.7.8"),
            ),
        )
        val reverseDnsLookupService = ReverseDnsLookupService(reverseDnsResolver)

        val result = reverseDnsLookupService.lookupHostNames("1.2.3.4")

        result.verifiedHostNames shouldBe listOf("verified.example")
        result.unverifiedHostNames shouldBe listOf("unverified.example")
    }

    "lookupHostNames returns empty lists when no PTR records exist" {
        val reverseDnsResolver = LookupServiceFakeReverseDnsResolver()
        val reverseDnsLookupService = ReverseDnsLookupService(reverseDnsResolver)

        val result = reverseDnsLookupService.lookupHostNames("1.2.3.4")

        result shouldBe ReverseDnsLookupResult()
        reverseDnsResolver.lookupAddressesCallCount shouldBe 0
    }


    "lookupHostNames limits number of host names verified" {
        val ptrRecords = (1..20).map { "host$it.example." }
        val reverseDnsResolver = LookupServiceFakeReverseDnsResolver(
            ptrRecordsByIpAddress = mapOf("1.2.3.4" to ptrRecords),
            addressesByHostName = ptrRecords.associate { it.trimEnd('.') to listOf("5.6.7.8") },
        )
        val reverseDnsLookupService = ReverseDnsLookupService(reverseDnsResolver)

        val result = reverseDnsLookupService.lookupHostNames("1.2.3.4")

        result.verifiedHostNames shouldBe emptyList()
        result.unverifiedHostNames.size shouldBe 10
        reverseDnsResolver.lookupAddressesCallCount shouldBe 10
    }

    "lookupHostNames returns host names not verified before the deadline as unverified" {
        val reverseDnsResolver = LookupServiceFakeReverseDnsResolver(
            ptrRecordsByIpAddress = mapOf(
                "1.2.3.4" to listOf("fast.example.", "slow.example.", "later.example.")
            ),
            addressesByHostName = mapOf(
                "fast.example" to listOf("1.2.3.4"),
                "slow.example" to listOf("1.2.3.4"),
                "later.example" to listOf("1.2.3.4"),
            ),
            addressLookupDelayByHostName = mapOf("slow.example" to Duration.ofSeconds(10)),
        )
        val reverseDnsLookupService = ReverseDnsLookupService(reverseDnsResolver, Duration.ofMillis(200))

        val startTime = System.nanoTime()
        val result = reverseDnsLookupService.lookupHostNames("1.2.3.4")

        Duration.ofNanos(System.nanoTime() - startTime).toMillis() shouldBeLessThan 2_000
        result.verifiedHostNames shouldBe listOf("fast.example")
        result.unverifiedHostNames shouldBe listOf("slow.example", "later.example")
        reverseDnsLookupService.destroy()
    }

    "lookupHostNames returns empty lists when the PTR lookup does not finish before the deadline" {
        val reverseDnsResolver = LookupServiceFakeReverseDnsResolver(
            ptrRecordsByIpAddress = mapOf("1.2.3.4" to listOf("host.example.")),
            ptrLookupDelay = Duration.ofSeconds(10),
        )
        val reverseDnsLookupService = ReverseDnsLookupService(reverseDnsResolver, Duration.ofMillis(200))

        val startTime = System.nanoTime()
        val result = reverseDnsLookupService.lookupHostNames("1.2.3.4")

        Duration.ofNanos(System.nanoTime() - startTime).toMillis() shouldBeLessThan 2_000
        result shouldBe ReverseDnsLookupResult()
        reverseDnsLookupService.destroy()
    }

    "lookupHostNames rejects lookups without waiting when all lookup slots are busy" {
        val ptrLookupStarted = CountDownLatch(1)
        val releasePtrLookup = CountDownLatch(1)
        val reverseDnsResolver = object : ReverseDnsResolver {
            override fun lookupPtrRecords(ipAddress: String): List<String> {
                ptrLookupStarted.countDown()
                releasePtrLookup.await()
                return emptyList()
            }

            override fun lookupAddresses(hostName: String): List<String> = emptyList()
        }
        val reverseDnsLookupService = ReverseDnsLookupService(
            reverseDnsResolver = reverseDnsResolver,
            lookupTimeout = Duration.ofSeconds(10),
            maxConcurrentLookups = 1,
        )
        val busyLookup = thread { reverseDnsLookupService.lookupHostNames("1.2.3.4") }
        try {
            ptrLookupStarted.await(5, TimeUnit.SECONDS) shouldBe true

            shouldThrow<ReverseDnsLookupCapacityException> {
                reverseDnsLookupService.lookupHostNames("5.6.7.8")
            }
        } finally {
            releasePtrLookup.countDown()
            busyLookup.join()
            reverseDnsLookupService.destroy()
        }
    }
})

private class LookupServiceFakeReverseDnsResolver(
    private val ptrRecordsByIpAddress: Map<String, List<String>> = emptyMap(),
    private val addressesByHostName: Map<String, List<String>> = emptyMap(),
    private val ptrLookupDelay: Duration = Duration.ZERO,
    private val addressLookupDelayByHostName: Map<String, Duration> = emptyMap(),
) : ReverseDnsResolver {
    @Volatile
    var lookupAddressesCallCount = 0

    override fun lookupPtrRecords(ipAddress: String): List<String> {
        Thread.sleep(ptrLookupDelay)
        return ptrRecordsByIpAddress[ipAddress] ?: emptyList()
    }

    override fun lookupAddresses(hostName: String): List<String> {
        lookupAddressesCallCount++
        Thread.sleep(addressLookupDelayByHostName[hostName] ?: Duration.ZERO)
        return addressesByHostName[hostName] ?: emptyList()
    }
}
