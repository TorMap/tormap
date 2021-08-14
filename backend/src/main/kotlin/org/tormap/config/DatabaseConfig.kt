package org.tormap.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding


@ConstructorBinding
@ConfigurationProperties("database")
data class DatabaseConfig(
    /**
     * The default name for a sequence used by the configured DB driver
     */
    val defaultSequenceName: String,

    /**
     * Relative resource file path to the ip2location DB file
     */
    val ip2locationResourceFile: String,

    /**
     * Whether caching should be enabled when using the IP lookup DBs
     */
    val shouldCacheIPLookup: Boolean,
)
