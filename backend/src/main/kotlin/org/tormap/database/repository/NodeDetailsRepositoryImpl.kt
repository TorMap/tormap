package org.tormap.database.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.tormap.adapter.dto.NodeFamilyIdentifiersDto
import org.tormap.adapter.dto.NodeIdentifiersDto
import javax.transaction.Transactional

interface NodeDetailsRepositoryImpl : NodeDetailsRepository {
    @Query("SELECT DISTINCT new org.tormap.database.repository.MonthFamilyMembersCount(month, count(familyId)) " +
            "FROM NodeDetails GROUP BY month ORDER BY month")
    fun findDistinctMonthFamilyMemberCount(): List<MonthFamilyMembersCount>

    @Query("SELECT DISTINCT month FROM NodeDetails WHERE autonomousSystemNumber IS NULL ORDER BY month")
    fun findDistinctMonthsAndAutonomousSystemNumberNull(): Set<String>

    @Query(
        "SELECT new org.tormap.adapter.dto.NodeIdentifiersDto(id, fingerprint, nickname) " +
            "FROM NodeDetails " +
            "WHERE id in :ids"
    )
    fun findNodeIdentifiers(ids: List<Long>): List<NodeIdentifiersDto>

    @Query(
        "SELECT new org.tormap.adapter.dto.NodeFamilyIdentifiersDto(familyId, count(id), function('LISTAGG', nickname, ', '), function('LISTAGG', autonomousSystemName, ', ')) " +
            "FROM NodeDetails " +
            "WHERE familyId in :ids " +
            "GROUP BY familyId"
    )
    fun findFamilyIdentifiers(ids: List<Long>): List<NodeFamilyIdentifiersDto>

    @Transactional
    @Modifying
    @Query("UPDATE NodeDetails SET familyId = null WHERE month = :month")
    fun clearFamiliesFromMonth(month: String): Int
}

class MonthFamilyMembersCount(
    val month: String,
    val count: Long,
)
