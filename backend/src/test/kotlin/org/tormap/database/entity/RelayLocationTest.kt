package org.tormap.database.entity

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.tormap.adapter.dto.RelayLocationDto
import org.torproject.descriptor.NetworkStatusEntry
import java.math.BigDecimal
import java.time.LocalDate

class RelayLocationTest : StringSpec({
    fun relayLocation(flags: Set<String>, bandwidth: Long = 1000, bandwidthWeights: Map<String, Int>? = null): RelayLocation {
        val entry = mockk<NetworkStatusEntry>()
        every { entry.fingerprint } returns "A".repeat(40)
        every { entry.flags } returns flags.toSortedSet()
        every { entry.bandwidth } returns bandwidth
        return RelayLocation(entry, LocalDate.of(2024, 3, 5), BigDecimal.ONE, BigDecimal.ONE, "DE", bandwidthWeights)
    }

    "flags are stored as ordinals" {
        relayLocation(setOf("Exit", "Guard", "Valid")).flags shouldBe "5, 7, 0"
    }

    "flags unknown to TorMap are skipped instead of dropping all flags" {
        val location = relayLocation(setOf("MiddleOnly", "Running", "Valid"))
        location.flags shouldBe "3, 0"
        RelayLocationDto(BigDecimal.ONE, BigDecimal.ONE, "DE", location.flags, null, null, null, null)
            .flags shouldBe listOf(3, 0)
    }

    "a relay without flags has none" {
        val location = relayLocation(emptySet())
        location.flags shouldBe ""
        RelayLocationDto(BigDecimal.ONE, BigDecimal.ONE, "DE", location.flags, null, null, null, null)
            .flags shouldBe null
    }

    "the weights are computed from the consensus entry" {
        val location = relayLocation(setOf("Guard", "Running", "Valid"), 3000, mapOf("Wgg" to 5000))
        location.consensusWeight shouldBe 3000
        location.guardWeight shouldBe 1500
    }

    "a consensus entry without bandwidth has no weights" {
        val location = relayLocation(setOf("Guard", "Running", "Valid"), -1, mapOf("Wgg" to 5000))
        location.consensusWeight shouldBe null
        location.guardWeight shouldBe null
    }
})
