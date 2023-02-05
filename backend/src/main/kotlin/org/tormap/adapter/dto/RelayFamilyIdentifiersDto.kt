package org.tormap.adapter.dto

data class RelayFamilyIdentifiersDto(
    val familyId: Long,
    val memberCount: Long,
    val nicknames: String?,
    val autonomousSystems: String?,
)
