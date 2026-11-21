package org.tormap.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.tormap.adapter.dto.v1.RelayV1Dto
import org.tormap.config.CacheConfig
import org.tormap.database.repository.RelayLocationRepositoryImpl
import tools.jackson.databind.ObjectMapper
import java.security.MessageDigest
import java.time.LocalDate
import java.util.HexFormat

/**
 * The relays of a day for the public API v1, with an ETag for conditional requests.
 *
 * The ETag is a hash of the JSON, computed once when the cache is filled. A shallow ETag filter would buffer and hash
 * the whole day (several MB) on every request, so it is not used for this endpoint (see AppConfig).
 */
@Service
class ApiV1RelaysOfDayService(
    private val relayLocationRepositoryImpl: RelayLocationRepositoryImpl,
    private val objectMapper: ObjectMapper,
) {
    class RelaysOfDay(val relays: List<RelayV1Dto>, val eTag: String)

    // Days without relays are not cached, so requests for made-up days can not evict the days that have data.
    // The key is the day as text, which CacheService uses to evict reprocessed days.
    @Cacheable(CacheConfig.API_V1_RELAYS_PER_DAY, key = "#day.toString()", unless = "#result.relays.isEmpty()")
    fun getRelaysOfDay(day: LocalDate): RelaysOfDay = load(day)

    /** Reads the relays of a day from the database, without the cache. CacheService uses it to warm up the cache. */
    fun load(day: LocalDate): RelaysOfDay {
        val relays = relayLocationRepositoryImpl.findAllV1UsingDay(day)
        val hash = MessageDigest.getInstance("SHA-256").digest(objectMapper.writeValueAsBytes(relays))
        // Weak, because the response may be compressed on the way
        return RelaysOfDay(relays, "W/\"${HexFormat.of().formatHex(hash, 0, 16)}\"")
    }
}
