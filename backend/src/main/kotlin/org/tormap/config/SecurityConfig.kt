package org.tormap.config

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
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
    private val actuatorPath: String
) {

    private val logger = KotlinLogging.logger { }

    @Bean
    fun passwordEncoderBean(): PasswordEncoder? {
        val encodingId = "bcrypt"
        val encoders = mapOf(encodingId to BCryptPasswordEncoder(12))
        return DelegatingPasswordEncoder(encodingId, encoders)
    }

    @Bean
    fun authenticationManagerBean(passwordEncoder: PasswordEncoder): InMemoryUserDetailsManager {
        val passwordFile = File(adminPasswordFile)
        var adminPassword = UUID.randomUUID().toString()
        if (passwordFile.exists()) {
            adminPassword = passwordFile.readText()
        } else {
            File(passwordFile.parent).mkdirs()
            passwordFile.createNewFile()
            passwordFile.writeText(adminPassword)
        }
        logger.info("------------------------------------------------------")
        logger.info("Admin password: $adminPassword")
        logger.info("------------------------------------------------------")

        val user = User.withUsername(adminUserName)
            .passwordEncoder(passwordEncoder::encode)
            .password(adminPassword)
            .roles("ADMIN")
            .build()
        return InMemoryUserDetailsManager(user)
    }

    @Bean
    fun securityFilterChainBean(http: HttpSecurity): SecurityFilterChain {
        // Require authenticated users for actuator endpoints
        http.authorizeHttpRequests()
            .requestMatchers("$actuatorPath/**")
            .authenticated()
            .and()
            .formLogin()

        // Disable spring security features for public facing endpoints
        http.authorizeHttpRequests()
            .requestMatchers("relay/**")
            .permitAll()
            .and()
            .headers().disable()
            .csrf().disable()

        return http.build()
    }
}
