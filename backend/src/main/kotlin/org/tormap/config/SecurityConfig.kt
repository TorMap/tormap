package org.tormap.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.tormap.logger
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*


@Configuration
@EnableWebSecurity
class SecurityConfig(
    @Value("\${spring.security.user.name}")
    private val adminUserName: String,

    @Value("\${spring.security.user.passwordFile}")
    private val adminPasswordFile: String,

    @Value("\${spring.h2.console.path}")
    private val h2ConsolePath: String,

    @Value("\${management.endpoints.web.base-path}")
    private val actuatorPath: String,
) : WebSecurityConfigurerAdapter() {
    private val logger = logger()

    @Throws(java.lang.Exception::class)
    override fun configure(auth: AuthenticationManagerBuilder) {
        val passwordFilePath = Path.of(adminPasswordFile)
        var password = UUID.randomUUID().toString()
        if (Files.exists(passwordFilePath)) {
            password = passwordFilePath.toFile().readText()
        } else {
            Files.createFile(passwordFilePath)
            passwordFilePath.toFile().writeText(password)
        }
        logger.info("------------------------------------------------------")
        logger.info("Admin password: $password")
        logger.info("------------------------------------------------------")

        auth.inMemoryAuthentication()
            .withUser(adminUserName)
            .password("{noop}$password")
            .roles("ADMIN")
    }

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        // Require authenticated users for actuator endpoints
        http.authorizeRequests()
            .antMatchers("$actuatorPath/**")
            .authenticated()

        // Require authenticated users for H2 database web console
        http.authorizeRequests()
            .antMatchers("$h2ConsolePath/**")
            .authenticated()
            .and()
            .csrf().disable()
            .headers().frameOptions().disable()

        // Enable unauthenticated access to all other endpoints and add a form login for authenticated ones
        http.authorizeRequests()
            .anyRequest().permitAll()
            .and()
            .formLogin()
    }
}
