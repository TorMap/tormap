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
import java.io.File
import java.util.*

@Configuration
@EnableWebSecurity
class SecurityConfig(
    @Value("\${spring.security.user.name}")
    private val adminUserName: String,

    @Value("\${spring.security.user.passwordFile}")
    private val adminPasswordFile: String,

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

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userDetailsService(passwordEncoder: PasswordEncoder): UserDetailsService {
        val passwordFile = File(adminPasswordFile)
        var rawPassword = UUID.randomUUID().toString()
        if (passwordFile.exists()) {
            rawPassword = passwordFile.readText().trim()
        } else {
            passwordFile.parentFile?.mkdirs()
            passwordFile.writeText(rawPassword)
        }
        logger.info("------------------------------------------------------")
        logger.info("Admin password is stored in file: ${adminPasswordFile}")
        logger.info("------------------------------------------------------")

        // If the file already contains a bcrypt hash, use it. Otherwise, encode the plaintext for in-memory storage.
        val isBcrypt = rawPassword.startsWith("$2a$") || rawPassword.startsWith("$2b$") || rawPassword.startsWith("$2y$")
        val hashedPassword = if (isBcrypt) rawPassword else passwordEncoder.encode(rawPassword)

        // Create the valid user that will be used to check the basic auth credentials
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
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type", "Accept", "X-Requested-With")
            exposedHeaders = listOf("Location")
            allowCredentials = false // Prefer tokens; set true only if you use cookies across origins
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
                    // Public API
                    .antMatchers("/", "/error", "/relay/**").permitAll()
                    // Allow OpenAPI/Swagger only when enabled
                    .apply {
                        if (swaggerEnabled) {
                            antMatchers("/openapi/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        }
                    }
                    // Admin-only actuator
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
