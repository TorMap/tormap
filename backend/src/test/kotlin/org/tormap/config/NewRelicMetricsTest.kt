package org.tormap.config

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.micrometer.registry.otlp.OtlpConfig
import io.micrometer.registry.otlp.OtlpMeterRegistry
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles

// Point the export at a closed local port so tests never send metrics to New Relic
@SpringBootTest(
    properties = [
        "NEW_RELIC_INGEST_KEY=test-key",
        "management.otlp.metrics.export.url=http://127.0.0.1:9/v1/metrics",
    ]
)
@ActiveProfiles("test")
class NewRelicMetricsEnabledTest(
    private val context: ApplicationContext,
) : StringSpec({
    "OTLP registry is created when NEW_RELIC_INGEST_KEY is set" {
        context.getBeansOfType(OtlpMeterRegistry::class.java).size shouldBe 1
    }

    "OTLP export carries the configured resource attributes" {
        val attributes = context.getBean(OtlpConfig::class.java).resourceAttributes()
        attributes shouldContain ("service.name" to "TorMap")
        attributes shouldContainKey "host.name"
    }
})

@SpringBootTest
@ActiveProfiles("test")
class NewRelicMetricsDisabledTest(
    private val context: ApplicationContext,
) : StringSpec({
    "no OTLP registry without NEW_RELIC_INGEST_KEY" {
        context.getBeansOfType(OtlpMeterRegistry::class.java).size shouldBe 0
    }
})
