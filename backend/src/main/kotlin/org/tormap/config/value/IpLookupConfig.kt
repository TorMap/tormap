package org.tormap.config.value

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.Resource

@ConfigurationProperties("ip-lookup")
data class IpLookupConfig(
    /**
     * Whether caching should be enabled when using the IP lookup DBs
     */
    val shouldCache: Boolean,

    /**
     * Config for looking up location
     */
    val locationLookup: LocationLookupConfig,

    /**
     * Config for looking up autonomous system
     */
    val autonomousSystemLookup: AutonomousSystemLookupConfig
)

data class LocationLookupConfig(

    /**
     * Relative backend path to the dbip DB file
     */
    val dbipDatabaseFile: Resource
)

data class AutonomousSystemLookupConfig(

    /**
     * Relative backend path to the MaxMind DB file
     */
    val maxmindDatabaseFile: Resource
)
