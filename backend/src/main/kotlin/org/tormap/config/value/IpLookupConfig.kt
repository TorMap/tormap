package org.tormap.config.value

import org.springframework.boot.context.properties.ConfigurationProperties

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
    val dbipDatabaseFile: String
)

data class AutonomousSystemLookupConfig(

    /**
     * Relative backend path to the MaxMind DB file
     */
    val maxmindDatabaseFile: String
)
