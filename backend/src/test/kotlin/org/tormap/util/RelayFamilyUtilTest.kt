package org.tormap.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.tormap.database.entity.RelayDetails
import org.tormap.mockRelayDetails
import kotlin.time.Duration.Companion.seconds

class RelayFamilyUtilTest : StringSpec({
    fun relay(letter: Char, familyEntries: String?) = mockRelayDetails(letter).apply { this.familyEntries = familyEntries }
    fun fingerprintEntry(letter: Char) = "$" + letter.toString().repeat(40)
    fun RelayFamilies.nicknames() = families.map { family -> family.map { it.nickname }.toSet() }

    "relays that list each other by fingerprint form a family" {
        val result = computeRelayFamilies(
            listOf(
                relay('A', fingerprintEntry('B')),
                relay('B', "${fingerprintEntry('A')}, ${fingerprintEntry('C')}"),
                // C does not confirm B
                relay('C', fingerprintEntry('D')),
            )
        )

        result.nicknames() shouldContainExactlyInAnyOrder listOf(setOf("A", "B"))
        result.confirmedConnectionCount shouldBe 2
        result.rejectedConnectionCount shouldBe 2
    }

    "nickname entries do not connect relays" {
        listOf(
            listOf(relay('A', fingerprintEntry('B')), relay('B', "A")),
            listOf(relay('A', "B"), relay('B', fingerprintEntry('A'))),
            listOf(relay('A', "B"), relay('B', "A")),
        ).forEach { relays ->
            val result = computeRelayFamilies(relays)

            result.families.shouldBeEmpty()
            result.rejectedConnectionCount shouldBe 2
        }
    }

    mapOf<String, (Char) -> String>(
        "lowercase hex" to { fingerprintEntry(it.lowercaseChar()) },
        "with =nickname" to { fingerprintEntry(it) + "=Relay$it" },
        "with ~nickname" to { fingerprintEntry(it) + "~Relay$it" },
        "lowercase hex with ~nickname" to { fingerprintEntry(it.lowercaseChar()) + "~Relay$it" },
    ).forEach { (format, entry) ->
        "fingerprint entries in other formats connect relays ($format)" {
            val invalidEntries = mutableListOf<String>()
            val result = computeRelayFamilies(
                listOf(relay('A', entry('B')), relay('B', entry('A')))
            ) { _, invalidEntry -> invalidEntries.add(invalidEntry) }

            result.nicknames() shouldContainExactlyInAnyOrder listOf(setOf("A", "B"))
            result.confirmedConnectionCount shouldBe 2
            invalidEntries.shouldBeEmpty()
        }
    }

    "families that are connected through a member are merged" {
        val result = computeRelayFamilies(
            listOf(
                relay('A', fingerprintEntry('B')),
                relay('C', fingerprintEntry('D')),
                relay('B', "${fingerprintEntry('A')}, ${fingerprintEntry('D')}"),
                relay('D', "${fingerprintEntry('C')}, ${fingerprintEntry('B')}"),
                relay('E', "${fingerprintEntry('F')}"),
                relay('F', "${fingerprintEntry('E')}"),
            )
        )

        result.nicknames() shouldContainExactlyInAnyOrder listOf(setOf("A", "B", "C", "D"), setOf("E", "F"))
    }

    "relays with a shared family key form a family" {
        val result = computeRelayFamilies(
            listOf(
                relay('A', null).apply { familyKeys = "ed25519:one" },
                relay('B', null).apply { familyKeys = "ed25519:one, ed25519:two" },
                relay('C', null).apply { familyKeys = "ed25519:two" },
                // A family key that no other relay has is no family
                relay('D', null).apply { familyKeys = "ed25519:three" },
                relay('E', null).apply { familyKeys = "ed25519:four" },
                relay('F', null).apply { familyKeys = "ed25519:four" },
            )
        )

        result.nicknames() shouldContainExactlyInAnyOrder listOf(setOf("A", "B", "C"), setOf("E", "F"))
        result.relaysWithFamilyKeyCount shouldBe 6
    }

    "family keys and family lines connect relays to one family" {
        val result = computeRelayFamilies(
            listOf(
                relay('A', fingerprintEntry('B')),
                relay('B', fingerprintEntry('A')).apply { familyKeys = "ed25519:one" },
                relay('C', null).apply { familyKeys = "ed25519:one" },
            )
        )

        result.nicknames() shouldContainExactlyInAnyOrder listOf(setOf("A", "B", "C"))
    }

    "a relay listing itself or unknown relays has no family" {
        val result = computeRelayFamilies(listOf(relay('A', "${fingerprintEntry('A')}, ${fingerprintEntry('Z')}")))

        result.families.shouldBeEmpty()
        result.rejectedConnectionCount shouldBe 2
    }

    listOf(
        " ",
        "A".repeat(20),
        "$" + "A".repeat(41),
        "$" + "A".repeat(40) + "=",
        "$" + "A".repeat(40) + "=" + "A".repeat(20),
        "$" + "A".repeat(40) + "-Alice",
        "A".repeat(40),
        "???",
    ).forEach {
        "invalid family entries are rejected ($it)" {
            val invalidEntries = mutableListOf<String>()
            val result = computeRelayFamilies(
                listOf(relay('A', "$it, ${fingerprintEntry('B')}"), relay('B', fingerprintEntry('A')))
            ) { _, entry -> invalidEntries.add(entry) }

            invalidEntries shouldBe listOf(it.trim())
            result.nicknames() shouldContainExactlyInAnyOrder listOf(setOf("A", "B"))
            result.rejectedConnectionCount shouldBe 1
        }
    }

    // RelayDetails has a constant hashCode, the computation took 17 minutes for a month of the real Tor network
    "computes large families quickly".config(timeout = 30.seconds) {
        val familySize = 200
        val relays = (0 until 4_000).map { index -> mockRelayDetails().apply { fingerprint = "%040X".format(index) } }
        relays.chunked(familySize).forEach { family ->
            val entries = family.joinToString(", ") { "$" + it.fingerprint }
            family.forEach { it.familyEntries = entries }
        }

        val result = computeRelayFamilies(relays)

        result.families.map(List<RelayDetails>::size) shouldBe List(relays.size / familySize) { familySize }
        result.confirmedConnectionCount shouldBe relays.size * (familySize - 1)
    }
})
