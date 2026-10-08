@file:Suppress("unused")

package io.kotest.provided

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.extensions.spring.SpringExtension

// The test database is started by the Testcontainers JDBC URL in src/test/resources/application-test.yml
object ProjectConfig : AbstractProjectConfig() {
    override val extensions: List<Extension> = listOf(SpringExtension())
}
