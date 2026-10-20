package org.tormap.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.tormap.database.repository.RelayDetailsRepositoryImpl
import org.tormap.database.repository.RelayLocationRepositoryImpl
import org.torproject.descriptor.Descriptor
import org.torproject.descriptor.DescriptorParseException
import org.torproject.descriptor.ServerDescriptor
import org.torproject.descriptor.UnparseableDescriptor
import java.io.File
import org.torproject.descriptor.NetworkStatusEntry
import org.torproject.descriptor.RelayNetworkStatusConsensus
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.SortedMap
import java.util.TreeMap

@SpringBootTest
@ActiveProfiles("test")
class DescriptorProcessingServiceTest(
    private val descriptorProcessingService: DescriptorProcessingService,
    private val relayLocationRepository: RelayLocationRepositoryImpl,
    private val relayDetailsRepository: RelayDetailsRepositoryImpl,
) : StringSpec({
    beforeEach {
        relayLocationRepository.deleteAll()
        relayDetailsRepository.deleteAll()
    }

    val day = LocalDate.of(2024, 3, 5)
    // Wgm is not 0, so a relay without the Guard flag shows that only guards get a guard weight
    val weights = mapOf("Wgg" to 5000, "Wgd" to 2500, "Wgm" to 5000)

    fun entry(letter: Char, flags: Set<String>, bandwidth: Long): NetworkStatusEntry {
        val entry = mockk<NetworkStatusEntry>()
        every { entry.fingerprint } returns letter.toString().repeat(40)
        // A public address, so that the GeoIP database knows a location
        every { entry.address } returns "1.1.1.1"
        every { entry.flags } returns flags.toSortedSet()
        every { entry.bandwidth } returns bandwidth
        return entry
    }

    fun consensus(vararg entries: NetworkStatusEntry): RelayNetworkStatusConsensus {
        val statusEntries: SortedMap<String, NetworkStatusEntry> = TreeMap()
        entries.forEach { statusEntries[it.fingerprint] = it }
        val consensus = mockk<RelayNetworkStatusConsensus>()
        every { consensus.validAfterMillis } returns day.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        every { consensus.statusEntries } returns statusEntries
        every { consensus.bandwidthWeights } returns TreeMap(weights)
        return consensus
    }

    "new relay locations get the consensus weight and the guard weight" {
        val result = descriptorProcessingService.processDescriptor(
            consensus(
                entry('A', setOf("Running", "Valid", "Guard"), 4000),
                entry('B', setOf("Running", "Valid", "Guard", "Exit"), 4000),
                entry('C', setOf("Running", "Valid"), 4000),
            )
        )
        result.error shouldBe null
        val byLetter = relayLocationRepository.findAll().associateBy { it.fingerprint[0] }
        byLetter.size shouldBe 3
        byLetter['A']!!.consensusWeight shouldBe 4000
        byLetter['A']!!.guardWeight shouldBe 2000
        byLetter['B']!!.guardWeight shouldBe 1000
        byLetter['C']!!.guardWeight shouldBe 0
    }

    "relay locations stored without weights get them when the consensus is processed again" {
        // Locations as stored before the weights were introduced
        descriptorProcessingService.processDescriptor(consensus(entry('A', setOf("Running", "Valid", "Guard"), 4000)))
        relayLocationRepository.findAll().forEach {
            it.consensusWeight = null
            it.guardWeight = null
            relayLocationRepository.save(it)
        }

        descriptorProcessingService.processDescriptor(
            consensus(
                entry('A', setOf("Running", "Valid", "Guard"), 6000),
                entry('B', setOf("Running", "Valid"), 100),
            )
        )

        val byLetter = relayLocationRepository.findAll().associateBy { it.fingerprint[0] }
        byLetter.size shouldBe 2
        byLetter['A']!!.consensusWeight shouldBe 6000
        byLetter['A']!!.guardWeight shouldBe 3000
        byLetter['B']!!.consensusWeight shouldBe 100
    }

    "weights of a relay location are not overwritten by later consensuses of the same day" {
        descriptorProcessingService.processDescriptor(consensus(entry('A', setOf("Running", "Valid", "Guard"), 4000)))
        descriptorProcessingService.processDescriptor(consensus(entry('A', setOf("Running", "Valid", "Guard"), 9000)))
        relayLocationRepository.findAll().single().consensusWeight shouldBe 4000
    }

    fun serverDescriptor(nickname: String, published: LocalDate, familyEntries: List<String>? = null): ServerDescriptor {
        val descriptor = mockk<ServerDescriptor>(relaxed = true)
        every { descriptor.fingerprint } returns "F".repeat(40)
        every { descriptor.nickname } returns nickname
        every { descriptor.address } returns "94.130.58.99"
        every { descriptor.publishedMillis } returns published.atTime(8, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        every { descriptor.platform } returns "Tor 0.4.8.12 on Linux"
        every { descriptor.contact } returns "x".repeat(300)
        every { descriptor.protocols } returns sortedMapOf("Link" to sortedSetOf(4, 5))
        every { descriptor.familyEntries } returns familyEntries
        every { descriptor.linkProtocolVersions } returns null
        every { descriptor.circuitProtocolVersions } returns null
        return descriptor
    }

    "server descriptors store the details of the relay per month" {
        val result = descriptorProcessingService.processDescriptor(
            serverDescriptor("first", day, listOf("\$${"A".repeat(40)}", "friend"))
        )

        result.yearMonth shouldBe "2024-03"
        result.error shouldBe null
        val relay = relayDetailsRepository.findAll().single()
        relay.month shouldBe "2024-03"
        relay.day shouldBe day
        relay.nickname shouldBe "first"
        relay.autonomousSystemNumber shouldBe 24940
        relay.familyEntries shouldBe "\$${"A".repeat(40)}, friend"
        relay.protocols shouldBe "Link (4, 5)"
        // Long untrusted values are truncated to fit the column
        relay.contact!!.length shouldBe 255
    }

    "a newer server descriptor of the month replaces the older one, an older one is ignored" {
        descriptorProcessingService.processDescriptor(serverDescriptor("first", day))
        descriptorProcessingService.processDescriptor(serverDescriptor("newer", day.plusDays(2)))
        descriptorProcessingService.processDescriptor(serverDescriptor("older", day.plusDays(1)))

        val relay = relayDetailsRepository.findAll().single()
        relay.nickname shouldBe "newer"
        relay.day shouldBe day.plusDays(2)

        // Another month gets its own entry
        descriptorProcessingService.processDescriptor(serverDescriptor("april", LocalDate.of(2024, 4, 1)))
        relayDetailsRepository.findAll().map { it.month }.toSet() shouldBe setOf("2024-03", "2024-04")
    }

    "unparsable descriptors are skipped without an error" {
        val descriptor = mockk<UnparseableDescriptor>()
        every { descriptor.descriptorFile } returns File("broken")
        every { descriptor.descriptorParseException } returns DescriptorParseException("broken")

        val result = descriptorProcessingService.processDescriptor(descriptor)
        result.yearMonth shouldBe null
        result.error shouldBe null
    }

    "unsupported descriptor types result in an error" {
        val descriptor = mockk<Descriptor>()
        every { descriptor.descriptorFile } returns File("other")

        descriptorProcessingService.processDescriptor(descriptor).error shouldBe
            "Descriptor type ${descriptor.javaClass.name} is not yet supported!"
    }
})
