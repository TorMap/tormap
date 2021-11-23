package org.tormap.service

import com.ip2location.IP2Location
import com.ip2location.IPResult
import org.springframework.stereotype.Service
import org.tormap.config.DatabaseConfig
import org.tormap.logger


/**
 * This service handles location tasks requiring a geo location database
 */
@Service
class IpLookupService(
    databaseConfig: DatabaseConfig,
) {
    private val logger = logger()
    private var ip2locationDatabaseReader: IP2Location? = IP2Location().open(
        databaseConfig.ip2locationDatabaseFile,
        databaseConfig.shouldCacheIPLookup,
    )

    /**
     * Get the approximate geo location of an [ipAddress]
     * by looking it up with two different file based DB providers (IP2Location & Maxmind)
     */
    fun getLocationForIpAddress(ipAddress: String): IPResult? = try {
        val ip2locationResult = ip2locationDatabaseReader!!.ipQuery(ipAddress)
        if (ip2locationResult.status != "OK") throw Exception(ip2locationResult.status)
        else if (ip2locationResult.containsEmptyLocation()) throw Exception("Location incomplete")
        ip2locationResult
    } catch (exception: Exception) {
        logger.debug("Location lookup failed for IP address $ipAddress! ${exception.message}")
        null
    }

    fun IPResult.containsEmptyLocation() =
        listOf(this.latitude, this.longitude, this.countryShort).any { it == null || it == "-"} ||
                (this.latitude == 0f && this.longitude == 0f)
}
