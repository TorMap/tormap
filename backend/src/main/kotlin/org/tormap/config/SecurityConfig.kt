package org.tormap.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
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
) : WebSecurityConfigurerAdapter() {
    private val logger = logger()

    @Throws(java.lang.Exception::class)
    override fun configure(auth: AuthenticationManagerBuilder) {
        val passwordFile = File(adminPasswordFile)
        var password = UUID.randomUUID().toString()
        if (passwordFile.exists()) {
            password = passwordFile.readText()
        } else {
            File(passwordFile.parent).mkdirs()
            passwordFile.createNewFile()
            passwordFile.writeText(password)
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
            .and()
            .formLogin()

        // Disable spring security features for public facing endpoints
        http.authorizeRequests()
            .antMatchers("relay/**")
            .permitAll()
            .and()
            .headers().disable()
            .csrf().disable()
    }
}
