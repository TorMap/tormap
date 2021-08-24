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
        else if (ip2locationResult.latitude == null || ip2locationResult.longitude == null) throw GeoException()
        ip2locationResult
    } catch (exception: Exception) {
        logger.warn("Location lookup failed for IP address $ipAddress! ${exception.javaClass}: ${exception.message}")
        null
    }
}

class GeoException : Exception("Latitude and longitude missing!")
