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
     * The rate at which archive relay consensus descriptors are handled.
     */
    val archiveRelayConsensuses: String,

    /**
     * The rate at which archive relay server descriptors are handled.
     */
    val archiveRelayServers: String,

    /**
     * The rate at which recent relay consensus descriptors are handled.
     */
    val recentRelayConsensuses: String,

    /**
     * The rate at which recent relay server descriptors are handled.
     */
    val recentRelayServers: String,

    /**
     * The rate at which the Autonomous System info in NodeDetails is updated.
     */
    val updateNodeAutonomousSystems: String,

    /**
     * The rate at which the families of NodeDetails are updated.
     */
    val updateNodeFamilies: String,

    /**
     * Whether all previously calculated families should be overridden
     */
    val shouldOverwriteFamilies: Boolean,
)
