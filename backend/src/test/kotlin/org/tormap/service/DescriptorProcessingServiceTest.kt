package org.tormap.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.tormap.database.repository.RelayLocationRepositoryImpl
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
) : StringSpec({
    beforeEach {
        relayLocationRepository.deleteAll()
    }

    val day = LocalDate.of(2024, 3, 5)
    val weights = mapOf("Wgg" to 5000, "Wgd" to 2500, "Wgm" to 0)

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
})
