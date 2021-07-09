package com.torusage.service

import com.ip2location.IP2Location
import com.maxmind.db.CHMCache
import com.maxmind.geoip2.DatabaseReader
import com.torusage.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.InetAddress


/**
 * This service handles location tasks requiring a geo location database
 */
@Service
class GeoLocationService(
    @Value("\${maxmind.db.resource.file}")
    private val maxmindDBFile: String,

    @Value("\${ip2location.db.resource.file}")
    private val ip2locationDBFile: String,

    @Value("\${geo.location.cache}")
    private val shouldCacheQueries: Boolean = true,
) {
    private val logger = logger()
    private var maxmindDatabaseReader: DatabaseReader? = null
    private var ip2locationDatabaseReader: IP2Location? = null

    init {
        val maxmindDBResource = javaClass.getResourceAsStream(maxmindDBFile) ?: throw GeoDatabaseNotFound(
            maxmindDBFile
        )
        val ip2locationDBResource = javaClass.getResource(ip2locationDBFile) ?: throw GeoDatabaseNotFound(
            ip2locationDBFile
        )
        var maxmindDatabaseReaderBuilder = DatabaseReader.Builder(maxmindDBResource)
        if (shouldCacheQueries) {
            maxmindDatabaseReaderBuilder = maxmindDatabaseReaderBuilder.withCache(CHMCache())
        }
        maxmindDatabaseReader = maxmindDatabaseReaderBuilder.build()
        ip2locationDatabaseReader = IP2Location().open(
            ip2locationDBResource.file, shouldCacheQueries
        )
    }

    /**
     * Get the approximate [GeoLocation] of an [ipAddress]
     * by looking it up with two different file based DB providers (IP2Location & Maxmind)
     */
    fun getLocationForIpAddress(ipAddress: String): GeoLocation? = try {
        val location = ip2locationDatabaseReader!!.ipQuery(ipAddress)
        if (location.status != "OK") throw Exception(location.status)
        else if (location.latitude == null || location.longitude == null) throw GeoException()
        GeoLocation(location.latitude!!.toBigDecimal(), location.longitude!!.toBigDecimal())
    } catch (exception: Exception) {
        logger.warn("IP2Location location lookup failed for IP address $ipAddress! ${exception.javaClass}: ${exception.message}")
        try {
            val location = maxmindDatabaseReader!!.city(InetAddress.getByName(ipAddress)).location
            if (location.latitude == null || location.longitude == null) throw GeoException()
            GeoLocation(location.latitude.toBigDecimal(), location.longitude.toBigDecimal())
        } catch (exception: Exception) {
            logger.warn("Maxmind location lookup failed for IP address $ipAddress! ${exception.javaClass}: ${exception.message}")
            null
        }
    }
}

class GeoLocation(
    rawLatitude: BigDecimal,
    rawLongitude: BigDecimal,
) {
    var latitude: BigDecimal = rawLatitude.setScale(4, RoundingMode.HALF_EVEN)
    var longitude: BigDecimal = rawLongitude.setScale(4, RoundingMode.HALF_EVEN)
}

class GeoException : Exception("Latitude and longitude missing!")
class GeoDatabaseNotFound(dbFile: String) : Exception(
    "A configured DB file could not be found: $dbFile"
)
