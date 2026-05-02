package org.tormap.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.CacheControl
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Duration
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Configuration
class HttpCachingConfig(
    @Value("\${app.http.cache.public.max-age-seconds:0}")
    private val publicCacheMaxAgeSeconds: Long,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(object : HandlerInterceptor {
            override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
                if (publicCacheMaxAgeSeconds <= 0 || request.method != HttpMethod.GET.name) return true

                val path = request.requestURI ?: return true

                // Only cache public JSON API endpoints. Do NOT add caching headers for actuator or docs.
                // Root endpoint ("/") intentionally uses Cache-Control: no-store.
                val isPublicApi = path.startsWith("/relay/")
                val isActuator = path.startsWith("/actuator")
                val isDocs = path.startsWith("/openapi") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger")

                if (!isPublicApi || isActuator || isDocs) return true

                response.setHeader(
                    HttpHeaders.CACHE_CONTROL,
                    CacheControl.maxAge(Duration.ofSeconds(publicCacheMaxAgeSeconds)).cachePublic().headerValue
                )
                // Useful for CDNs/proxies to vary correctly.
                response.setHeader(HttpHeaders.VARY, "Origin, Accept-Encoding")

                return true
            }
        })
    }
}
