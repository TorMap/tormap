package org.tormap.adapter.controller

import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.tormap.adapter.dto.RelayFamilyIdentifiersDto
import org.tormap.adapter.dto.RelayIdentifiersDto
import org.tormap.database.entity.RelayDetails
import org.tormap.database.repository.RelayDetailsRepository

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "The requested relay was not found.")
class RelayNotFoundException : RuntimeException()

@RestController
@RequestMapping("relay/details/")
class RelayDetailsController(val relayDetailsRepositoryImpl: RelayDetailsRepository) {
    @GetMapping("relay/{id}")
    fun getRelay(@PathVariable id: Long): RelayDetails =
        relayDetailsRepositoryImpl.findByIdOrNull(id) ?: throw RelayNotFoundException()

    @GetMapping("family/{id}")
    fun getFamily(@PathVariable id: Long): List<RelayDetails> = relayDetailsRepositoryImpl.findAllByFamilyId(id)

    @PostMapping("relay/identifiers")
    fun getRelayIdentifiers(@RequestBody ids: List<Long>): List<RelayIdentifiersDto> =
        relayDetailsRepositoryImpl.findRelayIdentifiers(ids)

    @PostMapping("family/identifiers")
    fun getFamilyIdentifiers(@RequestBody familyIds: List<Long>): List<RelayFamilyIdentifiersDto> =
        relayDetailsRepositoryImpl.findFamilyIdentifiers(familyIds)
}
