package org.tormap.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path

@ConfigurationProperties("descriptor")
data class DescriptorConfig(
    /**
     * The base url to download descriptors from
     */
    val apiBaseURL: String,

    /**
     * The local directory in which downloaded descriptors will be saved
     */
    val localDownloadDirectory: Path,

    /**
     * The sub path to download historic (starting 2007-10) relay consensus descriptors from
     */
    val archiveRelayConsensuses: String,

    /**
     * The sub path to download historic (starting 2005-12) relay server descriptors from
     */
    val archiveRelayServers: String,

    /**
     * The sub path to download recent relay consensus descriptors from
     */
    val recentRelayConsensuses: String,

    /**
     * The sub path to download recent relay server descriptors from
     */
    val recentRelayServers: String,
)
