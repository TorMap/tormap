package org.tormap.database.repository

import org.springframework.data.jpa.repository.Query
import org.tormap.adapter.controller.view.NodeFamilyIdentifiers
import org.tormap.adapter.controller.view.NodeIdentifiers

interface NodeDetailsRepositoryImpl : NodeDetailsRepository {
    @Query("SELECT DISTINCT month FROM NodeDetails ORDER BY month")
    fun findDistinctMonths(): Set<String>

    @Query("SELECT DISTINCT month FROM NodeDetails WHERE autonomousSystemNumber IS NULL ORDER BY month")
    fun findDistinctMonthsAndAutonomousSystemNumberNull(): Set<String>

    @Query("SELECT new org.tormap.adapter.controller.view.NodeIdentifiers(id, fingerprint, nickname) " +
            "FROM NodeDetails " +
            "WHERE id in :ids"
    )
    fun findNodeIdentifiers(ids: List<Long>): List<NodeIdentifiers>

    @Query("SELECT new org.tormap.adapter.controller.view.NodeFamilyIdentifiers(n.familyId, count(n.id), function('LISTAGG', n.fingerprint, ', '), function('LISTAGG', n.nickname, ', '), function('LISTAGG', a.autonomousSystemName, ', ')) " +
            "FROM NodeDetails n " +
            "LEFT JOIN AutonomousSystem a ON a.ipRange.ipFrom <= n.addressNumber and a.ipRange.ipTo >= n.addressNumber " +
            "WHERE n.familyId in :ids " +
            "GROUP BY n.familyId"
    )
    fun findFamilyIdentifiers(ids: List<Long>): List<NodeFamilyIdentifiers>
}
