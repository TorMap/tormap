package org.tormap.database.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import javax.sql.DataSource

/**
 * The public API v1 looks relays up by month and fingerprint. Both columns are `char`, so the unique
 * (month, fingerprint) index is only used when the values are bound as `varchar` (what JDBC's setString and Spring
 * Data's derived queries do). Bound as `text`, Postgres compares the columns as text and scans the whole table,
 * which takes over a second on a production-sized table. Sequential scans are disabled here, so the plan shows
 * whether the index can be used even on the small test table.
 */
@SpringBootTest
@ActiveProfiles("test")
class RelayDetailsIndexUsageTest(
    private val dataSource: DataSource,
) : StringSpec({

    fun planOf(sql: String): String = dataSource.connection.use { connection ->
        connection.autoCommit = false
        try {
            connection.createStatement().use { it.execute("SET LOCAL enable_seqscan = off") }
            connection.prepareStatement("EXPLAIN $sql").use { statement ->
                statement.setString(1, "2024-03")
                statement.setString(2, "A".repeat(40))
                statement.executeQuery().use { result ->
                    generateSequence { if (result.next()) result.getString(1) else null }.joinToString("\n")
                }
            }
        } finally {
            connection.rollback()
        }
    }

    "the lookup by month and fingerprint uses the unique index" {
        val plan = planOf("SELECT id FROM relay_details WHERE month = ? AND fingerprint = ?")
        plan shouldContain "Index"
        plan shouldNotContain "Seq Scan"
    }

    "values bound as text can not use the index" {
        planOf("SELECT id FROM relay_details WHERE month = ?::text AND fingerprint = ?::text") shouldContain "Seq Scan"
    }
})
