package org.tormap.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.support.incrementer.H2SequenceMaxValueIncrementer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.filter.ShallowEtagHeaderFilter
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
@EnableCaching
class AppConfig(
    private val dataSource: DataSource,

    @Value("\${spring.h2.defaultSequenceName}")
    private val h2DefaultSequenceName: String,
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
     * Access the H2 DB sequence incrementer to get the next sequence value
     */
    @Bean
    fun h2SequenceMaxValueIncrementer() = H2SequenceMaxValueIncrementer(dataSource, h2DefaultSequenceName)

    /**
     * Include an unique ETag hash for each response to enable client side caching.
     */
    @Bean
    fun shallowEtagHeaderFilter(): ShallowEtagHeaderFilter {
        val filter = ShallowEtagHeaderFilter();
        filter.isWriteWeakETag = true;
        return filter
    }
}
