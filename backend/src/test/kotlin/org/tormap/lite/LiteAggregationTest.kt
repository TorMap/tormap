package org.tormap.lite

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.tormap.adapter.dto.RelayLocationDto
import org.tormap.database.entity.TorRelayFlag
import java.math.BigDecimal

class LiteAggregationTest : StringSpec({
    fun relay(
        vararg flags: TorRelayFlag,
        country: String = "DE",
        familyId: Long? = null,
        asNumber: Int? = null,
        weight: Long? = null,
        latitude: Double = 0.0,
    ) = RelayLocationDto(
        BigDecimal.valueOf(latitude), BigDecimal.ZERO, country,
        flags.joinToString(", ") { it.ordinal.toString() }, 1, familyId, "relay", weight, asNumber,
    )

    val view = LiteView.parse(emptyMap())

    "filters relays by type and required flags" {
        val exit = relay(TorRelayFlag.Exit, TorRelayFlag.Stable)
        val guard = relay(TorRelayFlag.Guard)
        val other = relay()
        val relays = listOf(exit, guard, other)
        LiteAggregation.filterRelays(relays, view) shouldBe relays
        LiteAggregation.filterRelays(relays, view.copy(relayTypes = setOf(LiteRelayType.Guard, LiteRelayType.Other))) shouldBe listOf(guard, other)
        LiteAggregation.filterRelays(relays, view.copy(requiredFlags = setOf(TorRelayFlag.Stable))) shouldBe listOf(exit)
    }

    "keeps the relays with the highest weight only while sizing by weight" {
        val relays = (1L..20L).map { relay(weight = it) }
        LiteAggregation.filterRelays(relays, view.copy(topRelayCount = 10)) shouldBe relays
        LiteAggregation.filterRelays(relays, view.copy(sizeByWeight = true, topRelayCount = 10)) shouldBe relays.reversed().take(10)
    }

    "keeps all relays if none has a weight" {
        val relays = (1..20).map { relay() }
        LiteAggregation.keepRelaysWithHighestWeight(relays, 10) shouldBe relays
    }

    "drops selections of groups without relays" {
        val relays = listOf(relay(familyId = 1, asNumber = 7, country = "FR"))
        val selected = view.copy(selectedFamily = 1, selectedAutonomousSystem = 7, selectedCountry = "FR")
        LiteAggregation.normalized(selected, relays) shouldBe selected
        LiteAggregation.normalized(view.copy(selectedFamily = 2, selectedAutonomousSystem = 8, selectedCountry = "DE"), relays) shouldBe view
    }

    "counts the relays of the selection" {
        val relays = listOf(
            relay(TorRelayFlag.Exit, familyId = 1, asNumber = 7),
            relay(TorRelayFlag.Guard, familyId = 1, asNumber = 8),
            relay(familyId = 2, country = "FR"),
        )
        LiteAggregation.statistics(relays, view) shouldBe LiteStatistics(1, 1, 1, 2, 2, 2)
        LiteAggregation.statistics(relays, view.copy(selectedCountry = "DE")) shouldBe LiteStatistics(1, 1, 0, 1, 1, 2)
        LiteAggregation.statistics(relays, view.copy(selectedCountry = "DE")).totalCount shouldBe 2
    }

    "pads the circles of larger groups at a location like the frontend" {
        val relays = listOf(1L, 1L, 1L, 2L, 2L, 3L).map { relay(familyId = it) } + relay()
        val groups = LiteAggregation.groupsByLocation(relays) { it.familyId }.getValue(0.0 to 0.0)
        groups.map { it.groupId } shouldBe listOf(1L, 2L, 3L)
        groups.map { it.relays.size } shouldBe listOf(3, 2, 1)
        groups.map { it.padding } shouldBe listOf(5, 2, 0)
    }

    "ranks the largest groups" {
        val relays = listOf("DE", "DE", "FR", "US", "US", "US").map { relay(country = it) }
        LiteAggregation.largestGroups(relays, { it.country }, 2) shouldBe listOf<Pair<Any, Int>>("US" to 3, "DE" to 2)
    }
})
