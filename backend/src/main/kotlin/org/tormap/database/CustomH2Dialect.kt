package org.tormap.database

import org.hibernate.dialect.H2Dialect
import org.hibernate.dialect.function.StandardSQLFunction
import org.hibernate.type.StandardBasicTypes

/**
 * A custom dialect for the H2 database to advanced register functions
 */
@Suppress("unused")
class CustomH2Dialect: H2Dialect() {

    init {
        registerFunction("LISTAGG", StandardSQLFunction("LISTAGG", StandardBasicTypes.STRING))
    }

}
