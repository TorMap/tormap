package org.tormap

import io.mockk.every
import io.mockk.mockk
import org.tormap.database.entity.RelayDetails
import org.tormap.database.entity.RelayLocation
import org.torproject.descriptor.NetworkStatusEntry
import java.math.BigDecimal
import java.time.LocalDate

fun mockRelayDetails(mockLetter: Char = 'A') = RelayDetails(
    "2022",
    LocalDate.of(2022, 2, 4),
    mockLetter.toString(),
    null,
    "172.104.114.113",
    true,
    mockLetter.toString(),
    1073741824,
    1073741824,
    60223,
    null,
    null,
    mockLetter.toString().repeat(40),
    true,
    null,
    null,
    null,
    null,
    false,
    false,
    null,
    null,
    false,
)

fun mockRelayLocation(
    day: LocalDate = LocalDate.now(),
    mockLetter: Char = 'A',
    flags: Set<String> = emptySet(),
    consensusWeight: Long = -1,
    bandwidthWeights: Map<String, Int>? = null,
): RelayLocation {
    val networkStatusEntry = mockk<NetworkStatusEntry>()
    every { networkStatusEntry.fingerprint } returns mockLetter.toString().repeat(40)
    every { networkStatusEntry.flags } returns flags.toSortedSet()
    every { networkStatusEntry.bandwidth } returns consensusWeight
    return RelayLocation(
        networkStatusEntry,
        day,
        BigDecimal(50),
        BigDecimal(50),
        "DE",
        bandwidthWeights,
    )
}

