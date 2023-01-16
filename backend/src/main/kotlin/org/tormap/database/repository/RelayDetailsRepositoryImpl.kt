package org.tormap.database.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.tormap.adapter.dto.RelayFamilyIdentifiersDto
import org.tormap.adapter.dto.RelayIdentifiersDto
import org.tormap.database.entity.FIND_FAMILY_IDENTIFIERS_QUERY
import javax.transaction.Transactional


interface RelayDetailsRepositoryImpl : RelayDetailsRepository {
    @Query(
        "SELECT DISTINCT new org.tormap.database.repository.MonthFamilyMembersCount(month, count(familyId)) " +
            "FROM RelayDetails GROUP BY month ORDER BY month"
    )
    fun findDistinctMonthFamilyMemberCount(): List<MonthFamilyMembersCount>

    @Query("SELECT DISTINCT month FROM RelayDetails WHERE autonomousSystemNumber IS NULL ORDER BY month")
    fun findDistinctMonthsAndAutonomousSystemNumberNull(): Set<String>

    @Query(
        "SELECT new org.tormap.adapter.dto.RelayIdentifiersDto(id, fingerprint, nickname) " +
            "FROM RelayDetails " +
            "WHERE id in :ids"
    )
    fun findRelayIdentifiers(ids: List<Long>): List<RelayIdentifiersDto>

    @Query(
        name = FIND_FAMILY_IDENTIFIERS_QUERY,
        nativeQuery = true,
    )
    fun findFamilyIdentifiers(@Param("familyIds") familyIds: List<Long>): List<RelayFamilyIdentifiersDto>

    @Transactional
    @Modifying
    @Query("UPDATE RelayDetails SET familyId = null WHERE month = :month")
    fun clearFamiliesFromMonth(month: String): Int
}

class MonthFamilyMembersCount(
    val month: String,
    val count: Long,
)
