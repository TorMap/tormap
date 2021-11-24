package org.tormap.service

import com.ip2location.IP2Location
import com.ip2location.IPResult
import com.maxmind.db.CHMCache
import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.CityResponse
import org.springframework.stereotype.Service
import org.tormap.config.IpLookupConfig
import org.tormap.logger
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.InetAddress


/**
 * This service handles location tasks requiring a geo location database
 */
@Service
class IpLookupService(
    ipLookupConfig: IpLookupConfig,
) {
    private val logger = logger()
    private var ip2locationDatabaseReader: IP2Location = IP2Location().open(
        ipLookupConfig.locationLookup.ip2locationDatabaseFile,
        ipLookupConfig.shouldCache,
    )
    private var maxMindDatabaseReader =
        maxmindFormatDatabaseReader(ipLookupConfig.locationLookup.maxMindDatabaseFile, ipLookupConfig.shouldCache)
    private var dbipDatabaseReader =
        maxmindFormatDatabaseReader(ipLookupConfig.locationLookup.dbipDatabaseFile, ipLookupConfig.shouldCache)


    /**
     * Get the approximate geo location of an [ipAddress]
     * by looking it up with two different file based DB providers (IP2Location & Maxmind)
     */
    fun getLocationForIpAddress(ipAddress: String): Location? {
        val location = lookupLocationWithProvider(ipAddress, "dbip") {
            Location(dbipDatabaseReader.city(InetAddress.getByName(ipAddress)))
        } ?: lookupLocationWithProvider(ipAddress, "IP2Location") {
            val ip2locationResult = ip2locationDatabaseReader.ipQuery(ipAddress)
            if (ip2locationResult.status != "OK") throw Exception(ip2locationResult.status)
            Location(ip2locationResult)
        } ?: lookupLocationWithProvider(ipAddress, "MaxMind") {
            Location(maxMindDatabaseReader.city(InetAddress.getByName(ipAddress)))
        }
        if (location == null) {
            logger.warn("Location lookup for IP $ipAddress failed for all providers!")
        }
        return location
    }

    private fun lookupLocationWithProvider(ipAddress: String, lookupProvider: String, lookup: () -> Location) = try {
        lookup()
    } catch (exception: Exception) {
        logger.debug("Location lookup for IP $ipAddress with provider $lookupProvider failed! ${exception.javaClass}: ${exception.message}")
        null
    }

    private fun maxmindFormatDatabaseReader(databaseFilePath: String, shouldCache: Boolean) =
        if (shouldCache)
            DatabaseReader.Builder(File(databaseFilePath)).withCache(CHMCache()).build()
        else
            DatabaseReader.Builder(File(databaseFilePath)).build()
}

class Location {
    private val geoDecimalPlaces = 4
    var latitude: BigDecimal
    var longitude: BigDecimal
    var countryCode: String

    constructor (ip2locationResult: IPResult) {
        this.latitude = ip2locationResult.latitude!!.toBigDecimal().setScale(geoDecimalPlaces, RoundingMode.HALF_EVEN)
        this.longitude = ip2locationResult.longitude!!.toBigDecimal().setScale(geoDecimalPlaces, RoundingMode.HALF_EVEN)
        this.countryCode = ip2locationResult.countryShort!!
        this.ensureCompleteLocation()
    }

    constructor (maxMindCityResponse: CityResponse) {
        this.latitude =
            maxMindCityResponse.location.latitude.toBigDecimal().setScale(geoDecimalPlaces, RoundingMode.HALF_EVEN)
        this.longitude =
            maxMindCityResponse.location.longitude.toBigDecimal().setScale(geoDecimalPlaces, RoundingMode.HALF_EVEN)
        this.countryCode = maxMindCityResponse.country.isoCode
        this.ensureCompleteLocation()
    }

    private fun ensureCompleteLocation() {
        val locationIncomplete =
            this.countryCode == "-" || (this.latitude == BigDecimal.ZERO && this.longitude == BigDecimal.ZERO)
        if (locationIncomplete) {
            throw LocationIncompleteException("latitude=${latitude} longitude=${longitude} countryCode=${countryCode}")
        }
    }

    class LocationIncompleteException(message: String) : Exception(message)
}
