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
     * The rate at which relay consensus descriptors are handled.
     */
    val relayConsensusDescriptorsRate: String,

    /**
     * The rate at which relay server descriptors are handled.
     */
    val relayServerDescriptorsRate: String,

    /**
     * The rate at which the Autonomous System info in NodeDetails is updated.
     */
    val updateNodeAutonomousSystemsRate: String,

    /**
     * The rate at which the families of NodeDetails are updated.
     */
    val updateNodeFamiliesRate: String,

    /**
     * Whether all previously calculated families should be overridden
     */
    val updateNodeFamiliesOverwriteAll: Boolean,
)
