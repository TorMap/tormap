package org.tormap.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.cors.CorsConfiguration
import org.springframework.mock.web.MockHttpServletRequest

class AdminUserTest : StringSpec({
    val passwordEncoder = BCryptPasswordEncoder(4)
    fun securityConfig(allowedOrigins: String = "https://tormap.org") =
        SecurityConfig("admin", "/actuator", allowedOrigins, false)

    "an admin password creates the admin user with a hashed password" {
        val users = securityConfig().buildUserDetailsService(passwordEncoder, "  a-long-generated-password  ", null)
        val admin = users.loadUserByUsername("admin")
        admin.authorities.map { it.authority } shouldBe listOf("ROLE_ADMIN")
        // Surrounding whitespace, e.g. from an env file, is not part of the password
        passwordEncoder.matches("a-long-generated-password", admin.password) shouldBe true
        admin.password!!.startsWith("{") shouldBe false
    }

    "a short password still creates the admin user" {
        val users = securityConfig().buildUserDetailsService(passwordEncoder, "short", null)
        passwordEncoder.matches("short", users.loadUserByUsername("admin").password) shouldBe true
    }

    "without a password there is no user" {
        listOf(null, "", "   ").forEach { password ->
            val users = securityConfig().buildUserDetailsService(passwordEncoder, password, null)
            shouldThrow<UsernameNotFoundException> { users.loadUserByUsername("admin") }
        }
    }

    "the removed bcrypt variable does not create a user" {
        val hash = passwordEncoder.encode("a-long-generated-password")
        val users = securityConfig().buildUserDetailsService(passwordEncoder, null, hash)
        shouldThrow<UsernameNotFoundException> { users.loadUserByUsername("admin") }
    }

    "allowed CORS origins are read from a comma separated list" {
        val source = securityConfig(" https://a.example , ,https://*.b.example").corsConfigurationSource()
        val cors: CorsConfiguration = source.getCorsConfiguration(MockHttpServletRequest("GET", "/relay/location/days"))!!
        cors.allowedOriginPatterns shouldBe listOf("https://a.example", "https://*.b.example")
        cors.checkOrigin("https://x.b.example") shouldBe "https://x.b.example"
        cors.checkOrigin("https://evil.example") shouldBe null
        cors.allowCredentials shouldBe false
    }
})
