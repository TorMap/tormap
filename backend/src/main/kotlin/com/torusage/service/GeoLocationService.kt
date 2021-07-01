package com.torusage.service

import com.maxmind.db.CHMCache
import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.CityResponse
import com.maxmind.geoip2.record.Location
import com.torusage.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.InetAddress


/**
 * This service handles location tasks requiring a geo location database
 */
@Service
class GeoLocationService(
    @Value("\${geo.location.database.resource.file}")
    private val databaseResourceFile: String,
) {
    private val logger = logger()
    private val databaseReader: DatabaseReader = DatabaseReader.Builder(
        javaClass.getResourceAsStream(databaseResourceFile)
    ).withCache(CHMCache()).build()

    fun getCityLocationForIpAddress(ipAddress: String): CityResponse? = try {
        databaseReader.city(InetAddress.getByName(ipAddress))
    } catch (exception: Exception) {
        logger.warn("Location lookup failed for IP address $ipAddress! " + exception.message)
        null
    }

    fun getLocationForIpAddress(ipAddress: String): Location? = try {
        val location = databaseReader.city(InetAddress.getByName(ipAddress)).location
        if (location.latitude == null || location.longitude == null) throw Exception("Latitude and longitude were not found!")
        location
    } catch (exception: Exception) {
        logger.warn("Location lookup failed for IP address $ipAddress! " + exception.message)
        null
    }
}
