package org.tormap.database.repository

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.transaction.annotation.Transactional
import org.tormap.adapter.dto.RelayFamilyIdentifiersDto
import org.tormap.adapter.dto.RelayIdentifiersDto

interface RelayDetailsRepositoryImpl : RelayDetailsRepository {
    @Query(
        """SELECT month, count(DISTINCT family_id)
           FROM relay_details
           GROUP BY month
           ORDER BY month"""
    )
    fun findDistinctMonthFamilyMemberCount(): List<MonthFamilyMembersCount>

    @Query("SELECT DISTINCT month FROM relay_details WHERE autonomous_system_number IS NULL ORDER BY month")
    fun findDistinctMonthsAndAutonomousSystemNumberNull(): Set<String>

    @Query("SELECT id, fingerprint, nickname FROM relay_details WHERE id IN (:ids)")
    fun findRelayIdentifiers(ids: List<Long>): List<RelayIdentifiersDto>

    @Query(
        """SELECT family_id AS id,
                  count(id) AS memberCount,
                  string_agg(nickname, ', ') AS nicknames,
                  string_agg(DISTINCT autonomous_system_name, ', ') AS autonomousSystems
           FROM relay_details
           WHERE family_id IN (:familyIds)
           GROUP BY family_id"""
    )
    fun findFamilyIdentifiers(familyIds: List<Long>): List<RelayFamilyIdentifiersDto>

    @Modifying
    @Transactional
    @Query("UPDATE relay_details SET family_id = NULL WHERE month = :month")
    fun clearFamiliesFromMonth(month: String): Int
}

data class MonthFamilyMembersCount(val month: String, val count: Long)
