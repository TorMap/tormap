package org.tormap.service

import com.maxmind.db.CHMCache
import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.exception.AddressNotFoundException
import com.maxmind.geoip2.model.AsnResponse
import com.maxmind.geoip2.record.Country
import mu.KotlinLogging
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.tormap.config.IpLookupConfig
import java.io.File
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.InetAddress
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import kotlin.io.path.exists
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
    } catch (exception: AddressNotFoundException) {
        logger.debug(exception) { "Location lookup for IP $ipAddress with provider dbip failed!" }
        null
    }

    /**
     * Get the approximate geolocation of an [ipAddress]
     * by looking it up with two different file based DB providers (IP2Location & Maxmind)
     */
    fun lookupAutonomousSystem(ipAddress: String): AsnResponse? = try {
        maxmindAutonomousSystemDB.asn(InetAddress.getByName(ipAddress))
    } catch (e: AddressNotFoundException) {
        logger.debug(e) { "Autonomous System lookup for IP $ipAddress with provider MaxMind failed!" }
        null
    }

    /**
     * Checks if the provided Resource exists in the tmp folder. If not, it is stored. It is stored by its name, if the
     * resource changes, but not it's name, this change is not picked up and the old version is used.
     * Either the backing file has to be removed, a new name has to be used or the OS has to trigger a tmp file clean
     * up.
     * If the file is a tar.gz file, it's extracted.
     */
    private fun storeOrLoad(resource: Resource): File {
        // input stream is expensive, so make it lazy and only evaluate if we need it
        fun store(filename: String, inputStream: () -> InputStream): File {
            val tmpResourcePath = Path.of(System.getProperty("java.io.tmpdir"), filename)

            if (!tmpResourcePath.exists()) {
                Files.copy(inputStream(), tmpResourcePath)
            }

            return tmpResourcePath.toFile()
        }

        val filename = resource.filename!!
        return when {
            resource.isFile -> resource.file
            filename.endsWith(".gz") -> store(filename.removeSuffix(".gz")) { GZIPInputStream(resource.inputStream) }
            else -> store(filename, resource::getInputStream)
        }
    }

    /**
     * Create a database reader for the [.mmdb file format](https://maxmind.github.io/MaxMind-DB/)
     */
    private fun maxmindTypeDatabaseReader(databaseFile: Resource, shouldCache: Boolean): DatabaseReader {
        val file = storeOrLoad(databaseFile)

        val reader = DatabaseReader.Builder(file)
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
        require(countryCode != "-" && (latitude != BigDecimal.ZERO && longitude != BigDecimal.ZERO)) {
            "latitude=$latitude longitude=$longitude countryCode=$countryCode"
        }
    }
}
