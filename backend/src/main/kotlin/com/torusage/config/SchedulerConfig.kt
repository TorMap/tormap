package com.torusage.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding


@ConstructorBinding
@ConfigurationProperties("scheduler")
data class SchedulerConfig(
    /**
     * The rate in which relay consensus descriptors are handled
     * Please provide milliseconds or a java.time.Duration compliant term!
     * [https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-]
     */
    val relayConsensusDescriptorsRate: String,

    /**
     * The rate in which relay server descriptors are handled
     * Please provide milliseconds or a java.time.Duration compliant term!
     * [https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-]
     */
    val relayServerDescriptorsRate: String,
)
