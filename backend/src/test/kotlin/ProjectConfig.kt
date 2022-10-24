@file:Suppress("unused")

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.extensions.spring.SpringExtension
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName


@Testcontainers
object ProjectConfig: AbstractProjectConfig() {
    override fun extensions() = listOf(SpringExtension)

    @Container
    var database: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:14-alpine"))
        .withEnv("POSTGRES_USER", "postgres")
        .withEnv("POSTGRES_PASSWORD", "postgres")
        .withExposedPorts(5432)

    @DynamicPropertySource
    fun postgreSQLProperties(registry: DynamicPropertyRegistry) {
        registry.add("spring.datasource.username", database::getUsername)
        registry.add("spring.datasource.password", database::getPassword)
    }
}
