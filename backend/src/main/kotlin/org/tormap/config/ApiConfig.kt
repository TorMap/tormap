package org.tormap.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding


@ConstructorBinding
@ConfigurationProperties("api")
data class ApiConfig(
    /**
     * The base url to download descriptors from
     */
    val descriptorBaseURL: String,

    /**
     * The sub path to download relay consensus descriptors from
     */
    val descriptorPathRelayConsensuses: String,

    /**
     * The sub path to download relay server descriptors from
     */
    val descriptorPathRelayServers: String,

    /**
     * The local directory in which the downloaded descriptors will be saved
     */
    val descriptorDownloadDirectory: String,
)
