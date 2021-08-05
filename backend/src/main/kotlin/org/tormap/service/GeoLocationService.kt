package org.tormap.service

import com.ip2location.IP2Location
import com.maxmind.db.CHMCache
import com.maxmind.geoip2.DatabaseReader
import org.springframework.stereotype.Service
import org.tormap.config.DatabaseConfig
import org.tormap.logger
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.InetAddress


/**
 * This service handles location tasks requiring a geo location database
 */
@Service
class GeoLocationService(
    databaseConfig: DatabaseConfig,
) {
    private val logger = logger()
    private var maxmindDatabaseReader: DatabaseReader? = null
    private var ip2locationDatabaseReader: IP2Location? = null

    init {
        val maxmindDBResource =
            javaClass.getResourceAsStream(databaseConfig.maxmindResourceFile) ?: throw GeoDatabaseNotFound(
                databaseConfig.maxmindResourceFile
            )
        val ip2locationDBResource =
            javaClass.getResource(databaseConfig.ip2locationResourceFile) ?: throw GeoDatabaseNotFound(
                databaseConfig.ip2locationResourceFile
            )
        var maxmindDatabaseReaderBuilder = DatabaseReader.Builder(maxmindDBResource)
        if (databaseConfig.shouldCacheIPLookup) {
            maxmindDatabaseReaderBuilder = maxmindDatabaseReaderBuilder.withCache(CHMCache())
        }
        maxmindDatabaseReader = maxmindDatabaseReaderBuilder.build()
        ip2locationDatabaseReader = IP2Location().open(
            ip2locationDBResource.file, databaseConfig.shouldCacheIPLookup
        )
    }

    /**
     * Get the approximate [GeoLocation] of an [ipAddress]
     * by looking it up with two different file based DB providers (IP2Location & Maxmind)
     */
    fun getLocationForIpAddress(ipAddress: String): GeoLocation? = try {
        val ip2locationResult = ip2locationDatabaseReader!!.ipQuery(ipAddress)
        if (ip2locationResult.status != "OK") throw Exception(ip2locationResult.status)
        else if (ip2locationResult.latitude == null || ip2locationResult.longitude == null) throw GeoException()
        GeoLocation(
            ip2locationResult.latitude!!.toBigDecimal(),
            ip2locationResult.longitude!!.toBigDecimal(),
            ip2locationResult.countryShort,
        )
    } catch (exception: Exception) {
        logger.warn("IP2Location location lookup failed for IP address $ipAddress! ${exception.javaClass}: ${exception.message}")
        try {
            val maxmindResult = maxmindDatabaseReader!!.city(InetAddress.getByName(ipAddress))
            val location = maxmindResult.location
            if (location.latitude == null || location.longitude == null) throw GeoException()
            GeoLocation(
                location.latitude.toBigDecimal(),
                location.longitude.toBigDecimal(),
                maxmindResult.country.isoCode,
            )
        } catch (exception: Exception) {
            logger.warn("Maxmind location lookup failed for IP address $ipAddress! ${exception.javaClass}: ${exception.message}")
            null
        }
    }
}

class GeoLocation(
    rawLatitude: BigDecimal,
    rawLongitude: BigDecimal,
    val countryIsoCode: String?,
) {
    val latitude: BigDecimal = rawLatitude.setScale(4, RoundingMode.HALF_EVEN)
    val longitude: BigDecimal = rawLongitude.setScale(4, RoundingMode.HALF_EVEN)
}

class GeoException : Exception("Latitude and longitude missing!")
class GeoDatabaseNotFound(dbFile: String) : Exception(
    "A configured DB file could not be found: $dbFile"
)
