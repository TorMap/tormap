@file:Suppress("unused")

package org.tormap.adapter.controller.view

import org.tormap.commaSeparatedToList
import org.tormap.jointToCommaSeparated

/**
 * Identifiers of a family
 */
class NodeFamilyIdentifiers(
    val id: Long,
    val memberCount: Long,
    val fingerprints: String,
    val nicknames: String,
    autonomousSystems: String,
) {
    val autonomousSystems = autonomousSystems.commaSeparatedToList().toSet().jointToCommaSeparated()
}
