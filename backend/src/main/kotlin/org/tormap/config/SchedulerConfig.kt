package org.tormap.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding


@ConstructorBinding
@ConfigurationProperties("scheduler")
/**
 * For all following rates, please provide milliseconds or a [Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-) compliant term!
 */
data class SchedulerConfig(
    /**
     * The rate in which relay consensus descriptors are handled.
     */
    val relayConsensusDescriptorsRate: String,

    /**
     * The rate in which relay server descriptors are handled.
     */
    val relayServerDescriptorsRate: String,

    /**
     * The rate in which all node families should be updated
     */
    val updateAllNodeFamiliesRate: String,
)
