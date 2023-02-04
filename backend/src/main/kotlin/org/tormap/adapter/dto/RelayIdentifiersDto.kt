@file:Suppress("unused")

package org.tormap.adapter.dto

/**
 * Identifiers of a relay
 */
data class RelayIdentifiersDto(
    val id: Long,
    val fingerprint: String,
    val nickname: String,
)
