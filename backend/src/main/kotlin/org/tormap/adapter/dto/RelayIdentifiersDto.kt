@file:Suppress("unused")

package org.tormap.adapter.dto

/**
 * Identifiers of a relay
 */
class RelayIdentifiersDto(
    val id: Long,
    val fingerprint: String,
    val nickname: String,
)
