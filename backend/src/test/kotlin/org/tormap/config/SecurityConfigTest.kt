package org.tormap.config

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.filter.ForwardedHeaderFilter

@SpringBootTest
@ActiveProfiles("test")
class SecurityConfigTest(
    private val applicationContext: ApplicationContext,
    private val passwordEncoder: PasswordEncoder,
    private val userDetailsService: UserDetailsService,
    private val corsConfigurationSource: CorsConfigurationSource,
    private val forwardedHeaderFilter: ForwardedHeaderFilter,
    private val securityFilterChain: SecurityFilterChain
) : StringSpec({

    "security beans are loaded" {
        applicationContext shouldNotBe null
        passwordEncoder shouldNotBe null
        userDetailsService shouldNotBe null
        corsConfigurationSource shouldNotBe null
        forwardedHeaderFilter shouldNotBe null
        securityFilterChain shouldNotBe null
    }

})
