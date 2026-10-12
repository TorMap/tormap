package org.tormap.database.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.tormap.adapter.dto.CountryUsageDto
import org.tormap.adapter.dto.CountryUsageRow
import java.sql.Date
import java.sql.Types
import java.time.LocalDate

/**
 * Reads and writes the `country_usage` table. It is imported in bulk from a CSV, so plain SQL is used instead of entities.
 */
@Repository
class CountryUsageRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun findLatestDay(): LocalDate? =
        jdbcTemplate.queryForObject("SELECT max(day) FROM country_usage", Date::class.java)?.toLocalDate()

    fun findAllByDay(day: LocalDate): List<CountryUsageDto> =
        jdbcTemplate.query(
            "SELECT country_code, users, lower, upper, frac FROM country_usage WHERE day = ? ORDER BY country_code",
            { resultSet, _ ->
                CountryUsageDto(
                    country = resultSet.getString("country_code").trim(),
                    users = resultSet.getInt("users"),
                    lower = resultSet.getInt("lower").takeUnless { resultSet.wasNull() },
                    upper = resultSet.getInt("upper").takeUnless { resultSet.wasNull() },
                    frac = resultSet.getInt("frac"),
                )
            },
            Date.valueOf(day),
        )

    /**
     * Inserts the rows, rows of days and countries that exist are updated.
     */
    fun upsertAll(rows: List<CountryUsageRow>) {
        if (rows.isEmpty()) return
        jdbcTemplate.batchUpdate(
            "INSERT INTO country_usage (day, country_code, users, lower, upper, frac) VALUES (?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT (day, country_code) DO UPDATE SET " +
                    "users = EXCLUDED.users, lower = EXCLUDED.lower, upper = EXCLUDED.upper, frac = EXCLUDED.frac",
            rows,
            rows.size,
        ) { statement, row ->
            statement.setDate(1, Date.valueOf(row.day))
            statement.setString(2, row.countryCode)
            statement.setInt(3, row.users)
            if (row.lower != null) statement.setInt(4, row.lower) else statement.setNull(4, Types.INTEGER)
            if (row.upper != null) statement.setInt(5, row.upper) else statement.setNull(5, Types.INTEGER)
            statement.setInt(6, row.frac)
        }
    }
}
