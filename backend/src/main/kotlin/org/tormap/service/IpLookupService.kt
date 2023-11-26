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
        maxmindTypeDatabaseReader(
            javaClass.getResource(ipLookupConfig.locationLookup.dbipDatabaseFile)!!.path,
            ipLookupConfig.shouldCache
        )
    private var maxmindAutonomousSystemDB =
        maxmindTypeDatabaseReader(
            javaClass.getResource(ipLookupConfig.autonomousSystemLookup.maxmindDatabaseFile)!!.path,
            ipLookupConfig.shouldCache
        )

    fun lookupLocation(ipAddress: String): Location? = try {
        if (isValidPublicIPAddress(ipAddress)) {
            Location(dbipLocationDB.city(InetAddress.getByName(ipAddress)))
        } else null
    } catch (exception: Exception) {
        logger.debug("Location lookup for IP $ipAddress failed! ${exception.javaClass}: ${exception.message}")
        null
    }

    fun lookupAutonomousSystem(ipAddress: String): AsnResponse? = try {
        if (isValidPublicIPAddress(ipAddress)) {
            maxmindAutonomousSystemDB.asn(InetAddress.getByName(ipAddress))
        } else null
    } catch (exception: Exception) {
        logger.debug("Autonomous System lookup for IP $ipAddress failed! ${exception.javaClass}: ${exception.message}")
        null
    }

    private fun isValidPublicIPAddress(ipAddress: String) =
        isValidIPAddress(ipAddress) && !isPrivateIPAddress(ipAddress)

    private fun isValidIPAddress(ipAddress: String) =
        Regex("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}\$").matches(ipAddress)

    private fun isPrivateIPAddress(ipAddress: String): Boolean {
        val ipParts = ipAddress.split(".")
        val firstOctet = ipParts[0].toInt()

        when (firstOctet) {
            in 10..10 -> return true // 10.0.0.0 to 10.255.255.255
            in 172..172 -> {
                val secondOctet = ipParts[1].toInt()
                if (secondOctet in 16..31) {
                    return true // 172.16.0.0 to 172.31.255.255
                }
            }

            in 192..192 -> {
                val secondOctet = ipParts[1].toInt()
                if (secondOctet == 168) {
                    return true // 192.168.0.0 to 192.168.255.255
                }
            }
        }
        return false
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
