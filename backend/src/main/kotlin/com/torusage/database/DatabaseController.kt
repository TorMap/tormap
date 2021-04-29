package com.torusage.database

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class DatabaseController {
    val template = JdbcTemplate


}