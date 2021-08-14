package org.tormap.config

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.support.incrementer.H2SequenceMaxValueIncrementer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import javax.sql.DataSource

/**
 * Add all application configuration here and not in @SpringBootApplication class
 */
@Configuration
@EnableScheduling
@ConfigurationPropertiesScan
@EnableAsync
class AppConfig(
    val databaseConfig: DatabaseConfig,
    val dataSource: DataSource,
) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**").allowedMethods("*")
    }

    @Bean
    fun h2SequenceMaxValueIncrementer(): H2SequenceMaxValueIncrementer {
        return H2SequenceMaxValueIncrementer(dataSource, databaseConfig.defaultSequenceName)
    }

}
