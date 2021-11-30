package org.tormap.config.value

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding


@ConstructorBinding
@ConfigurationProperties("schedule")
data class ScheduleConfig(

    /**
     * Whether all previously calculated families should be overridden
     */
    val shouldOverwriteFamilies: Boolean,

    /**
     * In what interval tasks are run
     */
    val rate: RateConfig,
)

/**
 * For all following rates, please provide milliseconds or a [Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-) compliant term!
 */
data class RateConfig(
    /**
     * The rate at which recent relay consensus descriptors are handled.
     */
    val recentRelayConsensuses: String,

    /**
     * The rate at which recent relay server descriptors are handled.
     */
    val recentRelayServers: String,

    /**
     * The rate at which archive relay consensus descriptors are handled.
     */
    val archiveRelayConsensuses: String,

    /**
     * The rate at which archive relay server descriptors are handled.
     */
    val archiveRelayServers: String,

    /**
     * The rate at which missing Autonomous System info of relays is updated.
     */
    val updateRelayAutonomousSystems: String,

    /**
     * The rate at which missing families of relays are updated.
     */
    val updateRelayFamilies: String,
)
