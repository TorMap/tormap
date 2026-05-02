package org.tormap.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.ForwardedHeaderFilter
import org.tormap.util.logger

@Configuration
@EnableWebSecurity
class SecurityConfig(
    @Value("\${spring.security.user.name}")
    private val adminUserName: String,

    @Value("\${management.endpoints.web.base-path}")
    private val actuatorPath: String,

    // Comma-separated list of allowed frontend origins
    @Value("\${app.security.cors.allowed-origins:https://tormap.org,https://www.tormap.org}")
    private val allowedOriginsCsv: String,

    // Avoid exposing Swagger unless explicitly enabled
    @Value("\${springdoc.swagger-ui.enabled:false}")
    private val swaggerEnabled: Boolean,
) {
    private val logger = logger()

    companion object {
        private const val ENV_ADMIN_PASSWORD = "TORMAP_ADMIN_PASSWORD"
        private const val ENV_ADMIN_PASSWORD_BCRYPT = "TORMAP_ADMIN_PASSWORD_BCRYPT"

        private fun isBcrypt(passwordOrHash: String): Boolean =
            passwordOrHash.startsWith("$2a$") || passwordOrHash.startsWith("$2b$") || passwordOrHash.startsWith("$2y$")
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userDetailsService(passwordEncoder: PasswordEncoder): UserDetailsService {
        val bcryptFromEnv = System.getenv(ENV_ADMIN_PASSWORD_BCRYPT)?.trim().orEmpty()
        val passwordFromEnv = System.getenv(ENV_ADMIN_PASSWORD)?.trim().orEmpty()

        val secret = when {
            bcryptFromEnv.isNotBlank() -> bcryptFromEnv
            passwordFromEnv.isNotBlank() -> passwordFromEnv
            else -> ""
        }

        if (secret.isBlank()) {
            logger.warn("------------------------------------------------------")
            logger.warn("No admin password configured; actuator endpoints will not be accessible.")
            logger.warn("Set $ENV_ADMIN_PASSWORD (plaintext) or $ENV_ADMIN_PASSWORD_BCRYPT (bcrypt hash) to enable actuator admin access.")
            logger.warn("------------------------------------------------------")
            return InMemoryUserDetailsManager()
        }

        val hashedPassword = if (isBcrypt(secret)) secret else passwordEncoder.encode(secret)
        val user = User.withUsername(adminUserName)
            .password(hashedPassword)
            .roles("ADMIN")
            .build()

        return InMemoryUserDetailsManager(user)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val cors = CorsConfiguration().apply {
            allowedOrigins = allowedOriginsCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            // API is read-mostly; only advertise methods that exist.
            allowedMethods = listOf("GET", "POST", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type", "Accept", "X-Requested-With")
            exposedHeaders = listOf("Location")
            allowCredentials = false
            maxAge = 3600L
        }
        return UrlBasedCorsConfigurationSource().also { it.registerCorsConfiguration("/**", cors) }
    }

    @Bean
    fun forwardedHeaderFilter(): ForwardedHeaderFilter = ForwardedHeaderFilter()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors(Customizer.withDefaults())
            // Stateless API: disable CSRF
            .csrf { it.disable() }
            .authorizeRequests { auth ->
                auth
                    // Allow CORS preflight
                    .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // Public API and static resources
                    .antMatchers("/", "/error", "/relay/**", "/favicon.ico", "/robots.txt", "/static/**").permitAll()
                    // Allow OpenAPI/Swagger only when enabled
                    .apply {
                        if (swaggerEnabled) {
                            antMatchers(
                                "/openapi/**",
                                "/v3/api-docs/**",
                                "/swagger",
                                "/swagger/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                            ).permitAll()
                        }
                    }
                    // Admin-only actuator; will deny all if no users exist
                    .antMatchers("$actuatorPath/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            }
            // HTTP Basic for admin endpoints; no login pages
            .httpBasic(Customizer.withDefaults())
            .formLogin { it.disable() }
            // Fully stateless sessions
            .sessionManagement { sessions ->
                sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            // Security headers
            .headers { headers ->
                headers.defaultsDisabled()
                headers.xssProtection { it.block(true) }
                headers.contentTypeOptions()
                headers.frameOptions().deny()
                headers.referrerPolicy { it.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER) }
                if (!swaggerEnabled) {
                    headers.contentSecurityPolicy("default-src 'none'; frame-ancestors 'none'; base-uri 'none'")
                }
            }
        return http.build()
    }
}
