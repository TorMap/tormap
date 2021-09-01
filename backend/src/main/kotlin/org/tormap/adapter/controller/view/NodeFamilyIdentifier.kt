@file:Suppress("unused")

package org.tormap.adapter.controller.view

import org.tormap.commaSeparatedToList
import org.tormap.jointToCommaSeparated

/**
 * Identifier of a family
 */
class NodeFamilyIdentifier(
    val id: Long,
    val memberCount: Long,
    val nicknames: String,
    autonomousSystems: String?,
) {
    val autonomousSystems = autonomousSystems?.commaSeparatedToList()?.toSet()?.jointToCommaSeparated()
}
