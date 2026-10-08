package org.tormap.database.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.tormap.mockRelayDetails

/** Entity ids come from the shared `hibernate_sequence` (increment 1) created by the Flyway migration. */
@SpringBootTest
@ActiveProfiles("test")
class IdGenerationTest(
    private val relayDetailsRepository: RelayDetailsRepositoryImpl,
    private val jdbcTemplate: JdbcTemplate,
) : StringSpec({
    beforeEach {
        relayDetailsRepository.deleteAll()
    }

    "ids are consecutive values of the hibernate_sequence" {
        val first = relayDetailsRepository.save(mockRelayDetails('A'))
        val second = relayDetailsRepository.save(mockRelayDetails('B'))

        second.id!! shouldBe first.id!! + 1
        jdbcTemplate.queryForObject("SELECT last_value FROM hibernate_sequence", Long::class.java) shouldBe second.id
    }

})
