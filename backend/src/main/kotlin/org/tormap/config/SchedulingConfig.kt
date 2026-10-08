package org.tormap.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Enables the recurring descriptor and relay jobs. Disabled in tests so they do not download
 * descriptors and write into the test database while tests run.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "tormap.scheduling", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class SchedulingConfig
