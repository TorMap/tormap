package org.tormap.config

import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HttpExchangesConfig {
    // Spring Boot 3 no longer provides a default repository for the `httpexchanges` actuator endpoint (formerly `httptrace`)
    @Bean
    fun httpExchangeRepository(): HttpExchangeRepository = InMemoryHttpExchangeRepository()
}
