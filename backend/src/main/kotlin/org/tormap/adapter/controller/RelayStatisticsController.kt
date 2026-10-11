package org.tormap.adapter.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.tormap.adapter.dto.DailyRelayCountDto
import org.tormap.config.CacheConfig
import org.tormap.database.repository.RelayLocationRepositoryImpl

@RestController
@RequestMapping("relay/statistics/")
class RelayStatisticsController(
    val relayLocationRepositoryImpl: RelayLocationRepositoryImpl,
) {
    @Cacheable(CacheConfig.RELAY_DAILY_COUNTS, key = "T(org.tormap.config.CacheConfig).RELAY_DAILY_COUNTS_KEY")
    @Operation(summary = "Returns the number of relays (total, exit, guard, other) for every day with relay locations, ordered by day.")
    @GetMapping("daily-counts")
    fun getDailyCounts(): List<DailyRelayCountDto> = relayLocationRepositoryImpl.findDailyRelayCounts()
}
