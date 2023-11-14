package org.tormap.service

import com.maxmind.db.CHMCache
import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.AsnResponse
import com.maxmind.geoip2.model.CityResponse
import org.springframework.stereotype.Service
import org.tormap.config.value.IpLookupConfig
import org.tormap.util.logger
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.InetAddress

@Service
class IpLookupService(
    ipLookupConfig: IpLookupConfig,
) {
    private val logger = logger()
    private var dbipLocationDB =
        maxmindTypeDatabaseReader(javaClass.getResource(ipLookupConfig.locationLookup.dbipDatabaseFile)!!.path, ipLookupConfig.shouldCache)
    private var maxmindAutonomousSystemDB =
        maxmindTypeDatabaseReader(javaClass.getResource(ipLookupConfig.autonomousSystemLookup.maxmindDatabaseFile)!!.path, ipLookupConfig.shouldCache)

    fun lookupLocation(ipAddress: String): Location? = try {
        Location(dbipLocationDB.city(InetAddress.getByName(ipAddress)))
    } catch (exception: Exception) {
        logger.warn("Location lookup for IP $ipAddress with provider dbip failed! ${exception.javaClass}: ${exception.message}")
        null
    }

    fun lookupAutonomousSystem(ipAddress: String): AsnResponse? = try {
        maxmindAutonomousSystemDB.asn(InetAddress.getByName(ipAddress))
    } catch (exception: Exception) {
        logger.debug("Autonomous System lookup for IP $ipAddress with provider MaxMind failed! ${exception.javaClass}: ${exception.message}")
        null
    }

    private fun maxmindTypeDatabaseReader(databaseFilePath: String, shouldCache: Boolean) =
        if (shouldCache)
            DatabaseReader.Builder(File(databaseFilePath)).withCache(CHMCache()).build()
        else
            DatabaseReader.Builder(File(databaseFilePath)).build()
}

class Location(maxMindCityResponse: CityResponse) {
    private val geoDecimalPlaces = 4
    var latitude: BigDecimal
    var longitude: BigDecimal
    var countryCode: String

    init {
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
