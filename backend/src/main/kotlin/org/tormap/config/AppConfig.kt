package org.tormap.config

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.jdbc.support.incrementer.H2SequenceMaxValueIncrementer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.filter.ShallowEtagHeaderFilter
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.charset.StandardCharsets
import javax.sql.DataSource

/**
 * Add all application configuration here and not in @SpringBootApplication class
 */
@Configuration
@EnableScheduling
@ConfigurationPropertiesScan
@EnableAsync
@EnableCaching
class AppConfig(
    private val databaseConfig: DatabaseConfig,
    private val dataSource: DataSource,
) : WebMvcConfigurer {

    /**
     * Allow any origin for any route mapping
     */
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**")
            .allowedMethods("*")
            .allowedHeaders("*")
            .allowedOrigins("*")
    }

    /**
     * Set default Content-Type to application/json and disabled whitelabel error page by ignoring a clients accept
     * header.
     */
    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        val parameterMap: Map<String, String> = mapOf("charset" to StandardCharsets.UTF_8.name())

        configurer.apply {
            ignoreAcceptHeader(true)
            defaultContentType(MediaType(MediaType.APPLICATION_JSON, parameterMap))
        }
    }

    /**
     * Access the H2 DB sequence incrementer to get the next sequence value
     */
    @Bean
    fun h2SequenceMaxValueIncrementer() = H2SequenceMaxValueIncrementer(dataSource, databaseConfig.defaultSequenceName)

    /**
     * Include an unique ETag hash for each response to enable client side caching.
     */
    @Bean
    fun etag() = ShallowEtagHeaderFilter()
}
