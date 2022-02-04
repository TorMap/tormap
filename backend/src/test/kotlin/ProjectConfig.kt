@file:Suppress("unused")

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.extensions.spring.SpringExtension

object ProjectConfig: AbstractProjectConfig() {
    override fun extensions() = listOf(SpringExtension)
}