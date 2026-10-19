package org.tormap.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.tormap.util.logger

@Configuration
@EnableWebSecurity
class SecurityConfig(
    @param:Value("\${spring.security.user.name}")
    private val adminUserName: String,

    @param:Value("\${management.endpoints.web.base-path}")
    private val actuatorPath: String,

    // Comma-separated list of allowed frontend origins
    @param:Value("\${app.security.cors.allowed-origins:https://tormap.org,https://www.tormap.org,https://tormap-*.web.app}")
    private val allowedOriginPatternsCSV: String,

    // Avoid exposing Swagger unless explicitly enabled
    @param:Value("\${springdoc.swagger-ui.enabled:false}")
    private val swaggerEnabled: Boolean,
) {
    private val logger = logger()

    companion object {
        private const val ENV_ADMIN_PASSWORD = "TORMAP_ADMIN_PASSWORD"
        private const val ENV_REMOVED_ADMIN_PASSWORD_BCRYPT = "TORMAP_ADMIN_PASSWORD_BCRYPT"
        private const val MIN_ADMIN_PASSWORD_LENGTH = 16
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userDetailsService(passwordEncoder: PasswordEncoder): UserDetailsService {
        if (!System.getenv(ENV_REMOVED_ADMIN_PASSWORD_BCRYPT).isNullOrBlank()) {
            logger.warn("$ENV_REMOVED_ADMIN_PASSWORD_BCRYPT is no longer supported and is ignored. Use $ENV_ADMIN_PASSWORD instead.")
        }

        val password = System.getenv(ENV_ADMIN_PASSWORD)?.trim().orEmpty()

        if (password.isBlank()) {
            logger.warn("------------------------------------------------------")
            logger.warn("No admin password configured; actuator endpoints will not be accessible.")
            logger.warn("Set $ENV_ADMIN_PASSWORD to enable actuator admin access.")
            logger.warn("------------------------------------------------------")
            return InMemoryUserDetailsManager()
        }

        if (password.length < MIN_ADMIN_PASSWORD_LENGTH) {
            logger.warn("$ENV_ADMIN_PASSWORD is shorter than $MIN_ADMIN_PASSWORD_LENGTH characters. Use a long generated password.")
        }

        val user = User.withUsername(adminUserName)
            .password(passwordEncoder.encode(password))
            .roles("ADMIN")
            .build()

        return InMemoryUserDetailsManager(user)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val cors = CorsConfiguration().apply {
            allowedOriginPatterns = allowedOriginPatternsCSV.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            // API is read-mostly; only advertise methods that exist.
            allowedMethods = listOf("GET", "POST", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type", "Accept", "X-Requested-With")
            exposedHeaders = listOf("Location")
            allowCredentials = false
            maxAge = 3600L
        }
        return UrlBasedCorsConfigurationSource().also { it.registerCorsConfiguration("/**", cors) }
    }

    /** Admin actuator: the only chain that authenticates (HTTP Basic). */
    @Bean
    @Order(1)
    fun adminFilterChain(http: HttpSecurity): SecurityFilterChain {
        applyCommonConfig(http)
            .securityMatcher("$actuatorPath/**")
            // Browsers resend cached HTTP Basic credentials, so unsafe methods require the XSRF-TOKEN cookie value to be
            // echoed in the X-XSRF-TOKEN header (double submit, no server-side session). GET stays usable in a browser.
            .csrf { csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                csrf.csrfTokenRequestHandler(CsrfTokenRequestAttributeHandler())
            }
            // Will deny all if no users exist
            .authorizeHttpRequests { auth -> auth.anyRequest().hasRole("ADMIN") }
            // Form login is not configured, so there are no login pages
            .httpBasic(Customizer.withDefaults())
        return http.build()
    }

    /** Everything else: public paths without authentication, all other paths denied. */
    @Bean
    @Order(2)
    fun publicFilterChain(http: HttpSecurity): SecurityFilterChain {
        applyCommonConfig(http)
            // The frontend calls the public API from other origins
            .cors(Customizer.withDefaults())
            // No authentication, sessions or cookies on this chain, so there is nothing for CSRF to protect
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    // Allow CORS preflight
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // Public API and static resources
                    .requestMatchers("/", "/error", "/relay/**", "/favicon.ico", "/robots.txt", "/static/**").permitAll()
                    // Allow OpenAPI/Swagger only when enabled
                    .apply {
                        if (swaggerEnabled) {
                            requestMatchers(
                                "/openapi/**",
                                "/v3/api-docs/**",
                                "/swagger",
                                "/swagger/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                            ).permitAll()
                        }
                    }
                    .anyRequest().denyAll()
            }
        return http.build()
    }

    private fun applyCommonConfig(http: HttpSecurity): HttpSecurity =
        http
            // Fully stateless sessions
            .sessionManagement { sessions ->
                sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            // Security headers
            .headers { headers ->
                headers.defaultsDisabled()
                headers.xssProtection { it.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK) }
                headers.contentTypeOptions(Customizer.withDefaults())
                headers.frameOptions { it.deny() }
                headers.referrerPolicy { it.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER) }
                if (!swaggerEnabled) {
                    headers.contentSecurityPolicy { it.policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none'") }
                }
            }
}
