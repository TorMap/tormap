package org.tormap.config

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.servlet.FilterRegistrationBean
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
     * Include a weak ETag hash only for small, bounded GET endpoints.
     *
     * Shallow ETags require Spring to buffer the full response body before it can be hashed.
     * Keep this filter away from large public endpoints such as /relay/location/day/{day},
     * where buffering an entire day's relay locations can amplify memory and CPU usage.
     */
    @Bean
    fun shallowEtagHeaderFilterRegistration(): FilterRegistrationBean<ShallowEtagHeaderFilter> {
        val filter = ShallowEtagHeaderFilter()
        filter.isWriteWeakETag = true

        return FilterRegistrationBean(filter).apply {
            addUrlPatterns(
                "/relay/location/days",
                "/relay/details/relay/*",
                "/relay/details/family/*",
            )
        }
    }
}
