package org.tormap.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Duration

@Configuration
class WebConfig(
    @Value("\${app.http.cache.public.max-age-seconds:0}")
    private val publicCacheMaxAgeSeconds: Long,
) : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // Cache static resources (if any are served by the backend) in clients/CDN.
        // API responses are controlled via controller ResponseEntity headers.
        registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(org.springframework.http.CacheControl.maxAge(Duration.ofSeconds(publicCacheMaxAgeSeconds)).cachePublic())
    }
}

