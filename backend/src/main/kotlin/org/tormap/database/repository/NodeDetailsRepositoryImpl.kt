package org.tormap.database.repository

import org.springframework.data.jpa.repository.Query
import org.tormap.adapter.controller.view.NodeFamilyIdentifiers
import org.tormap.adapter.controller.view.NodeIdentifiers

interface NodeDetailsRepositoryImpl : NodeDetailsRepository {
    @Query("SELECT DISTINCT month FROM NodeDetails ORDER BY month")
    fun findDistinctMonths(): Set<String>

    @Query("SELECT new org.tormap.adapter.controller.view.NodeIdentifiers(id, fingerprint, nickname) " +
            "FROM NodeDetails " +
            "WHERE id in :ids"
    )
    fun findNodeIdentifiers(ids: List<Long>): List<NodeIdentifiers>

    @Query("SELECT new org.tormap.adapter.controller.view.NodeFamilyIdentifiers(familyId, count(id), function('LISTAGG', fingerprint, ', '), function('LISTAGG', nickname, ', ')) " +
            "FROM NodeDetails " +
            "WHERE familyId in :ids " +
            "GROUP BY familyId"
    )
    fun findFamilyIdentifiers(ids: List<Long>): List<NodeFamilyIdentifiers>
}
