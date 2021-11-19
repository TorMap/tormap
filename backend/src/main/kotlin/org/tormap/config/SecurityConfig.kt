package org.tormap.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    @Value("\${spring.h2.console.path}")
    private val h2ConsolePath: String,

    @Value("\${management.endpoints.web.base-path}")
    private val actuatorPath: String,
) : WebSecurityConfigurerAdapter() {

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
