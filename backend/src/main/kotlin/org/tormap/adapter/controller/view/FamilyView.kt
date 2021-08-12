@file:Suppress("unused")

package org.tormap.adapter.controller.view

import org.tormap.database.entity.IpLookupAs
import org.tormap.database.entity.NodeDetails

/**
 * Details about a family of nodes
 */
class FamilyView(
    val familyMembers: List<NodeDetails>,
    val autonomousSystems: Set<IpLookupAs>,
)
