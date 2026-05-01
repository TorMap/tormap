package org.tormap.service

import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.tormap.adapter.controller.exception.RelayNotFoundException
import org.tormap.adapter.dto.RelayDetailsDto
import org.tormap.adapter.dto.RelayIdentifiersDto
import org.tormap.database.entity.RelayDetails
import org.tormap.database.repository.RelayDetailsRepository

interface RelayDetailsLookup {
    fun findById(id: Long): RelayDetails?
    fun findAllByFamilyId(familyId: Long): List<RelayDetails>
}

@Component
class RelayDetailsRepositoryLookup(
    private val relayDetailsRepository: RelayDetailsRepository,
) : RelayDetailsLookup {
    override fun findById(id: Long): RelayDetails? = relayDetailsRepository.findById(id).orElse(null)

    override fun findAllByFamilyId(familyId: Long): List<RelayDetails> = relayDetailsRepository.findAllByFamilyId(familyId)
}

@Service
class RelayDetailsQueryService(
    private val relayDetailsLookup: RelayDetailsLookup,
    private val reverseDnsLookupService: ReverseDnsLookupService,
) {
    fun getRelay(id: Long): RelayDetailsDto {
        val details = relayDetailsLookup.findById(id) ?: throw RelayNotFoundException()
        return RelayDetailsDto(
            details = details,
            confirmedFamilyMembers = details.confirmedFamilyMembers(),
            reverseDnsLookupResult = reverseDnsLookupService.lookupHostNames(details.address),
        )
    }

    private fun RelayDetails.confirmedFamilyMembers(): List<RelayIdentifiersDto> {
        val currentRelayId = id ?: return emptyList()
        val currentFamilyId = familyId ?: return emptyList()
        return relayDetailsLookup.findAllByFamilyId(currentFamilyId)
            .asSequence()
            .filter { it.id != currentRelayId }
            .distinctBy { it.id }
            .sortedBy { it.nickname.lowercase() }
            .map { RelayIdentifiersDto(it.id!!, it.fingerprint, it.nickname) }
            .toList()
    }
}
