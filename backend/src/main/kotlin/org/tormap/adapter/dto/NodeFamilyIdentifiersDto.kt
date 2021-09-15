@file:Suppress("unused")

package org.tormap.adapter.dto

import org.tormap.commaSeparatedToList
import org.tormap.jointToCommaSeparated

/**
 * Identifier of a family
 */
class NodeFamilyIdentifiersDto(
    val id: Long,
    val memberCount: Long,
    val nicknames: String,
    autonomousSystems: String?,
) {
    val autonomousSystems = autonomousSystems?.commaSeparatedToList()?.toSet()?.jointToCommaSeparated()
}
