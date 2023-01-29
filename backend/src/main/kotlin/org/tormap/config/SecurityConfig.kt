package org.tormap.config

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
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
    @Value("\${spring.security.user.name}") private val adminUserName: String,
    @Value("\${spring.security.user.passwordFile}") private val adminPasswordFile: String,
    @Value("\${management.endpoints.web.base-path}") private val actuatorPath: String,
) {

    private val logger = KotlinLogging.logger { }

    @Bean
    fun passwordEncoderBean(): PasswordEncoder? {
        val encodingId = "bcrypt"
        val encoders = mapOf(encodingId to BCryptPasswordEncoder(12))
        return DelegatingPasswordEncoder(encodingId, encoders)
    }

    @Bean
    fun userDetailsServiceBean(passwordEncoder: PasswordEncoder): UserDetailsService {
        val passwordFile = File(adminPasswordFile)
        var adminPassword = UUID.randomUUID().toString()
        if (passwordFile.exists()) {
            adminPassword = passwordFile.readText()
        } else {
            File(passwordFile.parent).mkdirs()
            passwordFile.createNewFile()
            passwordFile.writeText(adminPassword)
        }
        logger.info { "------------------------------------------------------" }
        logger.info { "Admin password: $adminPassword" }
        logger.info { "------------------------------------------------------" }

        val user = User.withUsername(adminUserName)
            .passwordEncoder(passwordEncoder::encode)
            .password(adminPassword)
            .roles("ADMIN")
            .build()
        return InMemoryUserDetailsManager(user)
    }

    @Bean
    @Order(1)
    fun filterChainAdmin(http: HttpSecurity): SecurityFilterChain {
        http {
            securityMatcher("$actuatorPath/**")
            authorizeRequests {
                authorize(pattern = "$actuatorPath/**", access = hasRole("ADMIN"))
            }
            formLogin {}
        }

        return http.build()
    }

    @Bean
    @Order(2)
    fun filterChainBasic(http: HttpSecurity): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                authorize(access = permitAll)
            }
            headers {
                disable()
            }
            csrf {
                disable()
            }
        }

        return http.build()
    }
}
