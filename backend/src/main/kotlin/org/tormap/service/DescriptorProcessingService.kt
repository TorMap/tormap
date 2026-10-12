package org.tormap.service

import org.springframework.stereotype.Service
import org.tormap.database.entity.RelayDetails
import org.tormap.database.entity.RelayLocation
import org.tormap.database.repository.RelayDetailsRepository
import org.tormap.database.repository.RelayLocationRepositoryImpl
import org.tormap.util.logger
import org.tormap.util.PathSelection
import org.tormap.util.millisSinceEpochToLocalDate
import org.torproject.descriptor.Descriptor
import org.torproject.descriptor.RelayNetworkStatusConsensus
import org.torproject.descriptor.ServerDescriptor
import org.torproject.descriptor.UnparseableDescriptor
import java.time.YearMonth

@Service
class DescriptorProcessingService(
    private val relayLocationRepositoryImpl: RelayLocationRepositoryImpl,
    private val relayDetailsRepository: RelayDetailsRepository,
    private val ipLookupService: IpLookupService,
) {
    private val logger = logger()
    fun processDescriptor(descriptor: Descriptor): ProcessedDescriptorInfo {
        return try {
            return when (descriptor) {
                is RelayNetworkStatusConsensus -> processRelayConsensusDescriptor(descriptor)
                is ServerDescriptor -> processServerDescriptor(descriptor)
                is UnparseableDescriptor -> {
                    logger.debug("Unparsable descriptor in file {}: {}", descriptor.descriptorFile.name, descriptor.descriptorParseException.message)
                    ProcessedDescriptorInfo()
                }

                else -> throw Exception("Descriptor type ${descriptor.javaClass.name} is not yet supported!")
            }
        } catch (exception: Exception) {
            logger.error("Could not process descriptor part of {}! {}", descriptor.descriptorFile.name, exception.message)
            ProcessedDescriptorInfo(error = exception.message)
        }
    }

    private fun processRelayConsensusDescriptor(descriptor: RelayNetworkStatusConsensus): ProcessedDescriptorInfo {
        val descriptorDay = millisSinceEpochToLocalDate(descriptor.validAfterMillis)
        val existingFingerprintsForDay = relayLocationRepositoryImpl.findDistinctFingerprintsByDay(descriptorDay)
        // Locations stored before the weights were introduced get them from the first consensus of the day seen again
        val fingerprintsWithoutWeightForDay = when {
            existingFingerprintsForDay.isEmpty() -> emptySet()
            else -> relayLocationRepositoryImpl.findDistinctFingerprintsWithoutConsensusWeightByDay(descriptorDay)
        }
        val bandwidthWeights = descriptor.bandwidthWeights
        val relayLocationsToInsert = mutableListOf<RelayLocation>()
        val missingWeights = linkedMapOf<String, Pair<Long, Long?>>()
        descriptor.statusEntries.forEach {
            val networkStatusEntry = it.value
            if (existingFingerprintsForDay.contains(networkStatusEntry.fingerprint)) {
                if (fingerprintsWithoutWeightForDay.contains(networkStatusEntry.fingerprint)
                    && networkStatusEntry.bandwidth >= 0
                ) {
                    missingWeights[networkStatusEntry.fingerprint] = Pair(
                        networkStatusEntry.bandwidth,
                        PathSelection.guardWeight(networkStatusEntry.bandwidth, networkStatusEntry.flags, bandwidthWeights),
                    )
                }
                return@forEach
            }
            val location = ipLookupService.lookupLocation(networkStatusEntry.address)
            if (location != null) {
                relayLocationsToInsert.add(
                    RelayLocation(
                        networkStatusEntry,
                        descriptorDay,
                        location.latitude,
                        location.longitude,
                        location.countryCode,
                        bandwidthWeights,
                    )
                )
            }
        }
        relayLocationRepositoryImpl.saveAllAndFlush(relayLocationsToInsert)
        if (missingWeights.isNotEmpty()) {
            relayLocationRepositoryImpl.fillMissingConsensusWeights(
                descriptorDay,
                missingWeights.keys.joinToString(","),
                missingWeights.values.joinToString(",") { it.first.toString() },
                missingWeights.values.joinToString(",") { (it.second ?: -1).toString() },
            )
        }
        return ProcessedDescriptorInfo(YearMonth.from(descriptorDay).toString())
    }

    private fun processServerDescriptor(descriptor: ServerDescriptor): ProcessedDescriptorInfo {
        val descriptorDay = millisSinceEpochToLocalDate(descriptor.publishedMillis)
        val descriptorMonth = YearMonth.from(descriptorDay).toString()
        val existingRelay =
            relayDetailsRepository.findByMonthAndFingerprint(descriptorMonth, descriptor.fingerprint)
        if (existingRelay == null || existingRelay.day < descriptorDay) {
            val autonomousSystem = ipLookupService.lookupAutonomousSystem(descriptor.address)
            relayDetailsRepository.save(
                RelayDetails(
                    descriptor,
                    descriptorMonth,
                    descriptorDay,
                    autonomousSystem?.autonomousSystemOrganization(),
                    autonomousSystem?.autonomousSystemNumber()?.toInt(),
                    existingRelay?.id,
                )
            )
        }
        return ProcessedDescriptorInfo(descriptorMonth)
    }
}

class ProcessedDescriptorInfo(
    var yearMonth: String? = null,
    var error: String? = null,
)
