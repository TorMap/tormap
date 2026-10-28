package org.tormap.database.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import org.tormap.adapter.dto.DailyRelayCountDto
import org.tormap.adapter.dto.RelayLocationDto
import java.time.LocalDate


/**
 * Repository to interact with DB
 */
interface RelayLocationRepositoryImpl : RelayLocationRepository {
    @Query("SELECT DISTINCT day FROM RelayLocation ORDER BY day")
    fun findDistinctDays(): Set<LocalDate>

    @Query(
        "SELECT new org.tormap.adapter.dto.RelayLocationDto(g.latitude, g.longitude, g.countryCode, g.flags, n.id, n.familyId, n.nickname, g.consensusWeight, n.autonomousSystemNumber) FROM RelayLocation g " +
                "LEFT JOIN FETCH RelayDetails n " +
                "ON g.fingerprint = n.fingerprint " +
                "AND function('TO_CHAR', g.day, 'YYYY-MM') = n.month " +
                "WHERE g.day = :day"
    )
    fun findAllUsingDay(day: LocalDate): List<RelayLocationDto>

    /**
     * Flags are stored as comma separated enum ordinals, e.g. "3, 5, 7". The literals below are the ordinals of
     * [org.tormap.database.entity.TorRelayFlag.Exit] (5) and [org.tormap.database.entity.TorRelayFlag.Guard] (7),
     * which is verified by a test.
     */
    @Query(
        "SELECT new org.tormap.adapter.dto.DailyRelayCountDto(" +
                "g.day, " +
                "count(g), " +
                "sum(case when concat(', ', g.flags, ', ') like '%, 5, %' then 1 else 0 end), " +
                "sum(case when concat(', ', g.flags, ', ') like '%, 5, %' then 0 " +
                "when concat(', ', g.flags, ', ') like '%, 7, %' then 1 else 0 end)) " +
                "FROM RelayLocation g GROUP BY g.day ORDER BY g.day"
    )
    fun findDailyRelayCounts(): List<DailyRelayCountDto>

    @Query("SELECT DISTINCT fingerprint FROM RelayLocation WHERE day = :day")
    fun findDistinctFingerprintsByDay(day: LocalDate): Set<String>

    @Query("SELECT DISTINCT fingerprint FROM RelayLocation WHERE day = :day AND consensusWeight IS NULL")
    fun findDistinctFingerprintsWithoutConsensusWeightByDay(day: LocalDate): Set<String>

    /**
     * Fills in the weights of relay locations of [day] that do not have any yet. Existing weights are kept.
     * The three lists are parallel and comma separated, a guard weight of -1 stands for "unknown".
     * @return the number of updated relay locations
     */
    @Transactional
    @Modifying
    @Query(
        nativeQuery = true,
        value = "UPDATE relay_location r " +
                "SET consensus_weight = v.consensus_weight, guard_weight = NULLIF(v.guard_weight, -1) " +
                "FROM unnest(" +
                "string_to_array(:fingerprints, ','), " +
                "CAST(string_to_array(:consensusWeights, ',') AS bigint[]), " +
                "CAST(string_to_array(:guardWeights, ',') AS bigint[])" +
                ") AS v(fingerprint, consensus_weight, guard_weight) " +
                "WHERE r.day = :day AND r.fingerprint = v.fingerprint AND r.consensus_weight IS NULL"
    )
    fun fillMissingConsensusWeights(
        day: LocalDate,
        fingerprints: String,
        consensusWeights: String,
        guardWeights: String,
    ): Int
}
