package org.tormap.config.value

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
     * Database file for looking up location
     */
    val locationDatabaseFile: String,

    /**
     * Database file for looking up autonomous system
     */
    val autonomousSystemDatabaseFile: String,
)
