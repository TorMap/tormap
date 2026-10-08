package org.tormap.config

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micrometer.registry.otlp.OtlpMeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(properties = ["NEW_RELIC_INGEST_KEY=test-key"])
@ActiveProfiles("test")
class NewRelicMetricsEnabledTest(
    @Autowired private val context: ApplicationContext,
) : StringSpec({
    "OTLP registry is created when NEW_RELIC_INGEST_KEY is set" {
        context.getBeansOfType(OtlpMeterRegistry::class.java).size shouldBe 1
    }
})

@SpringBootTest
@ActiveProfiles("test")
class NewRelicMetricsDisabledTest(
    @Autowired private val context: ApplicationContext,
) : StringSpec({
    "no OTLP registry without NEW_RELIC_INGEST_KEY" {
        context.getBeansOfType(OtlpMeterRegistry::class.java).size shouldBe 0
    }
})
