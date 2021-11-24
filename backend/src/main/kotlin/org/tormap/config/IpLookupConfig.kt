package org.tormap.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding


@ConstructorBinding
@ConfigurationProperties("ip-lookup")
data class IpLookupConfig(
    /**
     * Whether caching should be enabled when using the IP lookup DBs
     */
    val shouldCache: Boolean,

    /**
     * Config for looking up location
     */
    val locationLookup: LocationLookupConfig
)

data class LocationLookupConfig(

    /**
     * Relative backend path to the ip2location DB file
     */
    val ip2locationDatabaseFile: String,

    /**
     * Relative backend path to the maxmind DB file
     */
    val maxMindDatabaseFile: String,

    /**
     * Relative backend path to the dbip DB file
     */
    val dbipDatabaseFile: String,
)
