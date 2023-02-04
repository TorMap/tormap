@file:Suppress("unused")

package org.tormap.adapter.dto

data class RelayFamilyIdentifiersDto(
    val id: Long,
    val memberCount: Long,
    val nicknames: String?,
    val autonomousSystems: String?,
)
