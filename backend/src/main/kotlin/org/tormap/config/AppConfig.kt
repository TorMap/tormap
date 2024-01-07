package org.tormap.config

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.filter.ShallowEtagHeaderFilter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Add all application configuration here and not in @SpringBootApplication class
 */
@Configuration
@EnableScheduling
@ConfigurationPropertiesScan
@EnableAsync
class AppConfig : WebMvcConfigurer {

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
     * Include an unique ETag hash for each response to enable client side caching.
     */
    @Bean
    fun shallowEtagHeaderFilter(): ShallowEtagHeaderFilter {
        val filter = ShallowEtagHeaderFilter()
        filter.isWriteWeakETag = true
        return filter
    }
}
