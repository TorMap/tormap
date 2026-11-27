package org.tormap.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import org.tormap.database.entity.RelayDetails
import org.tormap.database.entity.RelayLocation
import org.tormap.util.millisSinceEpochToLocalDate
import org.torproject.descriptor.DescriptorSourceFactory
import org.torproject.descriptor.RelayNetworkStatusConsensus
import org.torproject.descriptor.ServerDescriptor
import java.io.File
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Relay operators control most fields of the descriptors that TorMap downloads from Collector. This parses them like
 * DescriptorFileService does and builds the entities that DescriptorProcessingService would store.
 * Unparseable descriptors are fine, also when a parser bug made them unparseable (DescriptorParserImpl turns unexpected
 * exceptions into unparseable descriptors, see VENDORED.md). An exception that escapes the parser stops the import, one
 * from the entities drops a parsed relay, so both are findings, like a hang or excessive memory use.
 */
object DescriptorParserFuzzer {
    private val parser = DescriptorSourceFactory.createDescriptorParser()

    // Collector file names, the parser uses them as hints for some descriptor types
    private val fileNames = listOf("2026-11-01-00-00-00-consensus", "server-descriptors-2026-11", "descriptor")

    @JvmStatic
    fun fuzzerTestOneInput(data: FuzzedDataProvider) {
        val fileName = data.pickValue(fileNames)
        val descriptors = parser.parseDescriptors(data.consumeRemainingAsBytes(), File(fileName), fileName)
        for (descriptor in descriptors) {
            when (descriptor) {
                is ServerDescriptor -> {
                    val day = millisSinceEpochToLocalDate(descriptor.publishedMillis)
                    RelayDetails(descriptor, YearMonth.from(day).toString(), day, null, null, null)
                }

                is RelayNetworkStatusConsensus -> {
                    val day = millisSinceEpochToLocalDate(descriptor.validAfterMillis)
                    descriptor.statusEntries.values.forEach { entry ->
                        RelayLocation(entry, day, BigDecimal.ZERO, BigDecimal.ZERO, "DE", descriptor.bandwidthWeights)
                    }
                }
            }
        }
    }
}
