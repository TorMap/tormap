package org.tormap.config

import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.registry.otlp.OtlpMeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

/**
 * Metrics are exported to New Relic via OTLP (see `management.otlp.metrics.export` in application.yml).
 * The export is only enabled when the environment variable [ENV_INGEST_KEY] is set.
 */
class NewRelicMetricsEnvironmentPostProcessor : EnvironmentPostProcessor {
    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        if (!environment.getProperty(ENV_INGEST_KEY).isNullOrBlank()) {
            environment.propertySources.addFirst(
                MapPropertySource("newRelicMetrics", mapOf("management.otlp.metrics.export.enabled" to "true"))
            )
        }
    }

    companion object {
        const val ENV_INGEST_KEY = "NEW_RELIC_INGEST_KEY"
    }
}

@Configuration
class NewRelicMetricsConfig {
    @Bean
    fun otlpMeterFilters(): MeterRegistryCustomizer<OtlpMeterRegistry> = MeterRegistryCustomizer { registry ->
        registry.config().meterFilter(MeterFilter.ignoreTags("plz_ignore_me"))
        registry.config().meterFilter(MeterFilter.denyNameStartsWith("jvm.threads"))
    }
}
