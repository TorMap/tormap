@file:Suppress("unused")

package org.tormap.adapter.dto

import org.tormap.util.commaSeparatedToList
import org.tormap.util.jointToCommaSeparated

/**
 * Identifier of a family
 */
class RelayFamilyIdentifiersDto(
    val id: Long,
    val memberCount: Long,
    val nicknames: String,
    autonomousSystems: String?,
) {
    val autonomousSystems = autonomousSystems?.commaSeparatedToList()?.toSet()?.jointToCommaSeparated()
}
