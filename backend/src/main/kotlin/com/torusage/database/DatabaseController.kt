package com.torusage.database

//from https://www.developersoapbox.com/using-spring-jdbctemplate-with-kotlin/

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.sql.ResultSet

data class Beer(val id: Int, val name: String, val abv: Double)

@Service
class DatabaseController(@Autowired val jdbcTemplate: JdbcTemplate) {
     fun run() {

        //Create table ("IF NOT EXISTS" syntax may not be compatible with some databases):
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS favorite_beers(id INT, name VARCHAR(50), abv double precision)")

        //Insert some records:
        jdbcTemplate.execute("INSERT INTO favorite_beers(id, name,abv) VALUES(1, 'Lagunitas IPA', 6.2)")
        jdbcTemplate.execute("INSERT INTO favorite_beers(id, name,abv) VALUES(2, 'Jai Alai', 7.5)")


        //Declare rowmapper to map DB records to collection of Beer entities:
        var rowMapper: RowMapper<Beer> = RowMapper<Beer> { resultSet: ResultSet, rowIndex: Int ->
            Beer(resultSet.getInt("id"), resultSet.getString("name"), resultSet.getDouble("abv"))
        }
    }
}