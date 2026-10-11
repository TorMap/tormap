package org.tormap.database.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import org.tormap.adapter.dto.RelayLocationDto
import java.time.LocalDate


/**
 * Repository to interact with DB
 */
interface RelayLocationRepositoryImpl : RelayLocationRepository {
    @Query("SELECT DISTINCT day FROM RelayLocation ORDER BY day")
    fun findDistinctDays(): Set<LocalDate>

    @Query(
        "SELECT new org.tormap.adapter.dto.RelayLocationDto(g.latitude, g.longitude, g.countryCode, g.flags, n.id, n.familyId, n.nickname, g.consensusWeight) FROM RelayLocation g " +
                "LEFT JOIN FETCH RelayDetails n " +
                "ON g.fingerprint = n.fingerprint " +
                "AND function('TO_CHAR', g.day, 'YYYY-MM') = n.month " +
                "WHERE g.day = :day"
    )
    fun findAllUsingDay(day: LocalDate): List<RelayLocationDto>

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
