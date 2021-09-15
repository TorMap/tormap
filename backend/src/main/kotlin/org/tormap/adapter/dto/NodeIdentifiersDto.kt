@file:Suppress("unused")

package org.tormap.adapter.dto

/**
 * Identifiers of a node
 */
class NodeIdentifiersDto(
    val id: Long,
    val fingerprint: String,
    val nickname: String,
)
