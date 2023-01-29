package org.tormap.service

import com.maxmind.db.CHMCache
import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.AsnResponse
import com.maxmind.geoip2.record.Country
import mu.KotlinLogging
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.tormap.config.value.IpLookupConfig
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.InetAddress
import java.util.zip.GZIPInputStream
import com.maxmind.geoip2.record.Location as GeoIp2Location

/**
 * This service handles location tasks requiring a geolocation database
 */
@Service
class IpLookupService(ipLookupConfig: IpLookupConfig) {

    private val logger = KotlinLogging.logger { }

    private var dbipLocationDB = maxmindTypeDatabaseReader(
        ipLookupConfig.locationLookup.dbipDatabaseFile,
        ipLookupConfig.shouldCache
    )
    private var maxmindAutonomousSystemDB = maxmindTypeDatabaseReader(
        ipLookupConfig.autonomousSystemLookup.maxmindDatabaseFile,
        ipLookupConfig.shouldCache
    )

    /**
     * Get the approximate geolocation of an [ipAddress]
     * by looking it up with two different file based DB providers (IP2Location & Maxmind)
     */
    fun lookupLocation(ipAddress: String): Location? = try {
        val city = dbipLocationDB.city(InetAddress.getByName(ipAddress))
        Location(city.location, city.country)
    } catch (exception: Exception) {
        logger.warn(exception) { "Location lookup for IP $ipAddress with provider dbip failed!" }
        null
    }

    /**
     * Get the approximate geolocation of an [ipAddress]
     * by looking it up with two different file based DB providers (IP2Location & Maxmind)
     */
    fun lookupAutonomousSystem(ipAddress: String): AsnResponse? = try {
        maxmindAutonomousSystemDB.asn(InetAddress.getByName(ipAddress))
    } catch (exception: Exception) {
        logger.debug(exception) { "Autonomous System lookup for IP $ipAddress with provider MaxMind failed!" }
        null
    }

    /**
     * Create a database reader for the [.mmdb file format](https://maxmind.github.io/MaxMind-DB/)
     */
    private fun maxmindTypeDatabaseReader(databaseFile: Resource, shouldCache: Boolean): DatabaseReader {
        val inputStream = when {
            databaseFile.filename?.endsWith(".gz") == true -> GZIPInputStream(databaseFile.inputStream)
            else -> databaseFile.inputStream
        }

        val reader = DatabaseReader.Builder(inputStream)
        return if (shouldCache) reader.withCache(CHMCache()).build() else reader.build()
    }
}

class Location(location: GeoIp2Location, country: Country) {

    companion object {
        private const val geoDecimalPlaces = 4
    }

    val latitude: BigDecimal = location.latitude.toBigDecimal().setScale(geoDecimalPlaces, RoundingMode.HALF_EVEN)
    val longitude: BigDecimal = location.longitude.toBigDecimal().setScale(geoDecimalPlaces, RoundingMode.HALF_EVEN)
    val countryCode: String = country.isoCode

    init {
        val locationIncomplete = countryCode == "-" || (latitude == BigDecimal.ZERO && longitude == BigDecimal.ZERO)
        if (locationIncomplete) {
            throw LocationIncompleteException("latitude=$latitude longitude=$longitude countryCode=$countryCode")
        }
    }

    class LocationIncompleteException(message: String) : RuntimeException(message)
}
