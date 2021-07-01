package com.ip2location

import java.io.IOException
import java.io.RandomAccessFile
import java.math.BigInteger
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import java.util.regex.Pattern

/**
 * This class performs the lookup of com.ip2location.IP2Location data from an IP address by reading a BIN file.
 *
 *
 * Example usage scenarios:
 *
 *  * Redirect based on country
 *  * Digital rights management
 *  * Web log stats and analysis
 *  * Auto-selection of country on forms
 *  * Filter access from countries you do not do business with
 *  * Geo-targeting for increased sales and click-through
 *  * And much, much more!
 *
 * Copyright (c) 2002-2021 com.ip2location.IP2Location.com
 *
 * @author com.ip2location.IP2Location.com
 * @version 8.1.0
 */
class IP2Location {
    private var metaData: MetaData? = null

    private var ipV4Buffer: MappedByteBuffer? = null
    private var ipV6Buffer: MappedByteBuffer? = null
    private var mapDataBuffer: MappedByteBuffer? = null

    private val indexArrayIPV4 = Array(65536) { IntArray(2) }
    private val indexArrayIPV6 = Array(65536) { IntArray(2) }
    private var ipV4Offset: Long = 0
    private var ipV6Offset: Long = 0
    private var mapDataOffset: Long = 0
    private var ipV4ColumnSize = 0
    private var ipV6ColumnSize = 0

    /**
     * To use memory mapped file for faster queries, set to true.
     */
    private var useMemoryMappedFile = false

    /**
     * Sets the path for the BIN database (IPv4 BIN or IPv4+IPv6 BIN).
     */
    private var ipDatabasePath = ""

    private var countryPositionOffset = 0
    private var regionPositionOffset = 0
    private var cityPositionOffset = 0
    private var iSPPositionOffset = 0
    private var domainPositionOffset = 0
    private var zIPCodePositionOffset = 0
    private var latitudePositionOffset = 0
    private var longitudePositionOffset = 0
    private var timeZonePositionOffset = 0
    private var netSpeedPositionOffset = 0
    private var iDDCodePositionOffset = 0
    private var areaCodePositionOffset = 0
    private var weatherStationCodePositionOffset = 0
    private var weatherStationNamePositionOffset = 0
    private var mCCPositionOffset = 0
    private var mNCPositionOffset = 0
    private var mobileBrandPositionOffset = 0
    private var elevationPositionOffset = 0
    private var usageTypePositionOffset = 0
    private var addressTypePositionOffset = 0
    private var categoryPositionOffset = 0
    private var countryEnabled = false
    private var regionEnabled = false
    private var cityEnabled = false
    private var iSPEnabled = false
    private var latitudeEnabled = false
    private var longitudeEnabled = false
    private var domainEnabled = false
    private var zIPCodeEnabled = false
    private var timeZoneEnabled = false
    private var netSpeedEnabled = false
    private var iDDCodeEnabled = false
    private var areaCodeEnabled = false
    private var weatherStationCodeEnabled = false
    private var weatherStationNameEnabled = false
    private var mCCEnabled = false
    private var mNCEnabled = false
    private var mobileBrandEnabled = false
    private var elevationEnabled = false
    private var usageTypeEnabled = false
    private var addressTypeEnabled = false
    private var categoryEnabled = false

    /**
     * This function can be used to pre-load the BIN file.
     */
    @Throws(IOException::class)
    fun open(DBPath: String): IP2Location {
        ipDatabasePath = DBPath
        loadBIN()
        return this
    }

    /**
     * This function can be used to initialized the component with params and pre-load the BIN file.
     */
    @Throws(IOException::class)
    fun open(DBPath: String, UseMMF: Boolean) {
        useMemoryMappedFile = UseMMF
        open(DBPath)
    }

    /**
     * This function destroys the mapped bytes.
     */
    fun close() {
        metaData = null
        destroyMappedBytes()
    }

    private fun destroyMappedBytes() {
        ipV4Buffer = null
        ipV6Buffer = null
        mapDataBuffer = null
    }

    @Throws(IOException::class)
    private fun createMappedBytes() {
        var aFile: RandomAccessFile? = null
        try {
            aFile = RandomAccessFile(ipDatabasePath, "r")
            val inChannel = aFile.channel
            createMappedBytes(inChannel)
        } finally {
            aFile?.close()
        }
    }

    @Throws(IOException::class)
    private fun createMappedBytes(inChannel: FileChannel) {
        if (ipV4Buffer == null) {
            val ipV4Bytes = ipV4ColumnSize.toLong() * metaData!!.dbCount.toLong()
            ipV4Offset = metaData!!.baseAddr - 1.toLong()
            ipV4Buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, ipV4Offset, ipV4Bytes)
            ipV4Buffer?.order(ByteOrder.LITTLE_ENDIAN)
            mapDataOffset = ipV4Offset + ipV4Bytes
        }

        if (!metaData!!.oldBIN && ipV6Buffer == null) {
            val ipV6Bytes = ipV6ColumnSize.toLong() * metaData!!.dbCountIPV6.toLong()
            ipV6Offset = metaData!!.baseAddrIPV6 - 1.toLong()
            ipV6Buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, ipV6Offset, ipV6Bytes)
            ipV6Buffer?.order(ByteOrder.LITTLE_ENDIAN)
            mapDataOffset = ipV6Offset + ipV6Bytes
        }
        if (mapDataBuffer == null) {
            mapDataBuffer =
                    inChannel.map(FileChannel.MapMode.READ_ONLY, mapDataOffset, inChannel.size() - mapDataOffset)
            mapDataBuffer?.order(ByteOrder.LITTLE_ENDIAN)
        }
    }

    @Throws(IOException::class)
    private fun loadBIN(): Boolean {
        var loadOK = false
        var aFile: RandomAccessFile? = null
        try {
            if (ipDatabasePath.isNotEmpty()) {
                aFile = RandomAccessFile(ipDatabasePath, "r")
                val inChannel = aFile.channel
                val headerBuffer =
                        inChannel.map(FileChannel.MapMode.READ_ONLY, 0, 64) // 64 bytes header
                headerBuffer.order(ByteOrder.LITTLE_ENDIAN)
                metaData = MetaData()
                metaData!!.dbType = headerBuffer[0].toInt()
                metaData!!.dbColumn = headerBuffer[1].toInt()
                metaData!!.dbYear = headerBuffer[2].toInt()
                metaData!!.dbMonth = headerBuffer[3].toInt()
                metaData!!.dbDay = headerBuffer[4].toInt()
                metaData!!.dbCount = headerBuffer.getInt(5) // 4 bytes
                metaData!!.baseAddr = headerBuffer.getInt(9) // 4 bytes
                metaData!!.dbCountIPV6 = headerBuffer.getInt(13) // 4 bytes
                metaData!!.baseAddrIPV6 = headerBuffer.getInt(17) // 4 bytes
                metaData!!.indexBaseAddr = headerBuffer.getInt(21) //4 bytes
                metaData!!.indexBaseAddrIPV6 = headerBuffer.getInt(25) //4 bytes
                metaData!!.productCode = headerBuffer[29].toInt()
                metaData!!.productType = headerBuffer[30].toInt()
                metaData!!.fileSize = headerBuffer.getInt(31) //4 bytes

                // check if is correct BIN (should be 1 for com.ip2location.IP2Location BIN file), also checking for zipped file (PK being the first 2 chars)
                if ((metaData!!.productCode != 1 && metaData!!.dbYear >= 21) || (metaData!!.dbType == 80 && metaData!!.dbColumn == 75)) { // only BINs from Jan 2021 onwards have this byte set
                    throw IOException("Incorrect com.ip2location.IP2Location BIN file format. Please make sure that you are using the latest com.ip2location.IP2Location BIN file.")
                }

                if (metaData!!.indexBaseAddr > 0) {
                    metaData?.indexed = true
                }
                if (metaData!!.dbCountIPV6 == 0) { // old style IPv4-only BIN file
                    metaData!!.oldBIN = true
                } else {
                    if (metaData!!.indexBaseAddrIPV6 > 0) {
                        metaData!!.indexedIPV6 = true
                    }
                }
                val dbColl: Int = metaData!!.dbColumn
                ipV4ColumnSize = dbColl shl 2 // 4 bytes each column
                ipV6ColumnSize = 16 + (dbColl - 1 shl 2) // 4 bytes each column, except IPFrom column which is 16 bytes
                val dbType: Int = metaData!!.dbType

                countryPositionOffset = if (COUNTRY_POSITION[dbType] != 0) COUNTRY_POSITION[dbType] - 2 shl 2 else 0
                regionPositionOffset = if (REGION_POSITION[dbType] != 0) REGION_POSITION[dbType] - 2 shl 2 else 0
                cityPositionOffset = if (CITY_POSITION[dbType] != 0) CITY_POSITION[dbType] - 2 shl 2 else 0
                iSPPositionOffset = if (ISP_POSITION[dbType] != 0) ISP_POSITION[dbType] - 2 shl 2 else 0
                domainPositionOffset = if (DOMAIN_POSITION[dbType] != 0) DOMAIN_POSITION[dbType] - 2 shl 2 else 0
                zIPCodePositionOffset = if (ZIPCODE_POSITION[dbType] != 0) ZIPCODE_POSITION[dbType] - 2 shl 2 else 0
                latitudePositionOffset = if (LATITUDE_POSITION[dbType] != 0) LATITUDE_POSITION[dbType] - 2 shl 2 else 0
                longitudePositionOffset = if (LONGITUDE_POSITION[dbType] != 0) LONGITUDE_POSITION[dbType] - 2 shl 2 else 0
                timeZonePositionOffset = if (TIMEZONE_POSITION[dbType] != 0) TIMEZONE_POSITION[dbType] - 2 shl 2 else 0
                netSpeedPositionOffset = if (NETSPEED_POSITION[dbType] != 0) NETSPEED_POSITION[dbType] - 2 shl 2 else 0
                iDDCodePositionOffset = if (IDDCODE_POSITION[dbType] != 0) IDDCODE_POSITION[dbType] - 2 shl 2 else 0
                areaCodePositionOffset = if (AREACODE_POSITION[dbType] != 0) AREACODE_POSITION[dbType] - 2 shl 2 else 0
                weatherStationCodePositionOffset = if (WEATHERSTATIONCODE_POSITION[dbType] != 0) WEATHERSTATIONCODE_POSITION[dbType] - 2 shl 2 else 0
                weatherStationNamePositionOffset = if (WEATHERSTATIONNAME_POSITION[dbType] != 0) WEATHERSTATIONNAME_POSITION[dbType] - 2 shl 2 else 0
                mCCPositionOffset = if (MCC_POSITION[dbType] != 0) MCC_POSITION[dbType] - 2 shl 2 else 0
                mNCPositionOffset = if (MNC_POSITION[dbType] != 0) MNC_POSITION[dbType] - 2 shl 2 else 0
                mobileBrandPositionOffset = if (MOBILEBRAND_POSITION[dbType] != 0) MOBILEBRAND_POSITION[dbType] - 2 shl 2 else 0
                elevationPositionOffset = if (ELEVATION_POSITION[dbType] != 0) ELEVATION_POSITION[dbType] - 2 shl 2 else 0
                usageTypePositionOffset = if (USAGETYPE_POSITION[dbType] != 0) USAGETYPE_POSITION[dbType] - 2 shl 2 else 0
                addressTypePositionOffset = if (ADDRESSTYPE_POSITION[dbType] != 0) ADDRESSTYPE_POSITION[dbType] - 2 shl 2 else 0
                categoryPositionOffset = if (CATEGORY_POSITION[dbType] != 0) CATEGORY_POSITION[dbType] - 2 shl 2 else 0
                countryEnabled = COUNTRY_POSITION[dbType] != 0
                regionEnabled = REGION_POSITION[dbType] != 0
                cityEnabled = CITY_POSITION[dbType] != 0
                iSPEnabled = ISP_POSITION[dbType] != 0
                latitudeEnabled = LATITUDE_POSITION[dbType] != 0
                longitudeEnabled = LONGITUDE_POSITION[dbType] != 0
                domainEnabled = DOMAIN_POSITION[dbType] != 0
                zIPCodeEnabled = ZIPCODE_POSITION[dbType] != 0
                timeZoneEnabled = TIMEZONE_POSITION[dbType] != 0
                netSpeedEnabled = NETSPEED_POSITION[dbType] != 0
                iDDCodeEnabled = IDDCODE_POSITION[dbType] != 0
                areaCodeEnabled = AREACODE_POSITION[dbType] != 0
                weatherStationCodeEnabled = WEATHERSTATIONCODE_POSITION[dbType] != 0
                weatherStationNameEnabled = WEATHERSTATIONNAME_POSITION[dbType] != 0
                mCCEnabled = MCC_POSITION[dbType] != 0
                mNCEnabled = MNC_POSITION[dbType] != 0
                mobileBrandEnabled = MOBILEBRAND_POSITION[dbType] != 0
                elevationEnabled = ELEVATION_POSITION[dbType] != 0
                usageTypeEnabled = USAGETYPE_POSITION[dbType] != 0
                addressTypeEnabled = ADDRESSTYPE_POSITION[dbType] != 0
                categoryEnabled = CATEGORY_POSITION[dbType] != 0
                if (metaData!!.indexed) {
                    // reading indexes
                    val indexBuffer = inChannel.map(
                            FileChannel.MapMode.READ_ONLY,
                            metaData!!.indexBaseAddr - 1.toLong(),
                            metaData!!.baseAddr - metaData!!.indexBaseAddr.toLong()
                    )
                    indexBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    var pointer = 0

                    // read IPv4 index
                    for (x in indexArrayIPV4.indices) {
                        indexArrayIPV4[x][0] = indexBuffer.getInt(pointer) // 4 bytes for from row
                        indexArrayIPV4[x][1] = indexBuffer.getInt(pointer + 4) // 4 bytes for to row
                        pointer += 8
                    }
                    if (metaData!!.indexedIPV6) {
                        // read IPv6 index
                        for (x in indexArrayIPV6.indices) {
                            indexArrayIPV6[x][0] = indexBuffer.getInt(pointer) // 4 bytes for from row
                            indexArrayIPV6[x][1] = indexBuffer.getInt(pointer + 4) // 4 bytes for to row
                            pointer += 8
                        }
                    }
                }
                if (useMemoryMappedFile) {
                    createMappedBytes(inChannel)
                } else {
                    destroyMappedBytes()
                }
                loadOK = true
            }
        } finally {
            aFile?.close()
        }
        return loadOK
    }

    /**
     * This function to query com.ip2location.IP2Location data.
     * @param IPAddress IP Address you wish to query
     * @return com.ip2location.IP2Location data
     */
    @Throws(IOException::class)
    fun ipQuery(IPAddress: String?): IPResult {
        val ipAddress = IPAddress?.trim { it <= ' ' } ?: "" // if null, it becomes empty string
        val record = IPResult(ipAddress)
        var fileHandle: RandomAccessFile? = null
        var myBuffer: ByteBuffer? = null
        var myDataBuffer: ByteBuffer? = null
        try {
            if (ipAddress.isEmpty()) {
                record.status = "EMPTY_IP_ADDRESS"
                return record
            }
            var ipNo: BigInteger
            val indexAddr: Int
            val actualIPType: Int
            var myIPType: Int
            var myBaseAddr = 0
            val myColumnSize: Int
            var myBufCapacity = 0
            val maxIPRange: BigInteger
            var rowOffset: Long
            var rowOffset2: Long
            val bi: Array<BigInteger>
            var overCapacity = false
            val retArr: Array<String>
            try {
                bi = ip2No(ipAddress)
                myIPType = bi[0].toInt()
                ipNo = bi[1]
                actualIPType = bi[2].toInt()
                if (actualIPType == 6) { // means didn't match IPv4 regex
                    retArr = expandIPV6(ipAddress, myIPType)
                    record.ipAddress = retArr[0] // return after expand IPv6 format
                    myIPType = retArr[1].toInt() // special cases
                }
            } catch (e: UnknownHostException) {
                record.status = "INVALID_IP_ADDRESS"
                return record
            }
            var low: Long = 0
            var high: Long
            var mid: Long
            var position: Long
            var ipFrom: BigInteger
            var ipTo: BigInteger

            // Read BIN if haven't done so
            if (metaData == null) {
                if (!loadBIN()) { // problems reading BIN
                    record.status = "MISSING_FILE"
                    return record
                }
            }

            if (useMemoryMappedFile) {
                if (ipV4Buffer == null || !metaData!!.oldBIN && ipV6Buffer == null || mapDataBuffer == null) {
                    createMappedBytes()
                }
            } else {
                destroyMappedBytes()
                fileHandle = RandomAccessFile(ipDatabasePath, "r")
            }
            if (myIPType == 4) { // IPv4
                maxIPRange = MAX_IPV4_RANGE
                high = metaData!!.dbCount.toLong()
                if (useMemoryMappedFile) {
                    myBuffer = ipV4Buffer!!.duplicate() // this enables this thread to maintain its own position in a multi-threaded environment
                    myBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    myBufCapacity = myBuffer.capacity()
                } else {
                    myBaseAddr = metaData!!.baseAddr
                }
                myColumnSize = ipV4ColumnSize
                if (metaData!!.indexed) {
                    indexAddr = ipNo.shiftRight(16).toInt()
                    low = indexArrayIPV4[indexAddr][0].toLong()
                    high = indexArrayIPV4[indexAddr][1].toLong()
                }
            } else { // IPv6
                if (metaData!!.oldBIN) {
                    record.status = "IPV6_NOT_SUPPORTED"
                    return record
                }
                maxIPRange = MAX_IPV6_RANGE
                high = metaData!!.dbCountIPV6.toLong()
                if (useMemoryMappedFile) {
                    myBuffer = ipV6Buffer!!.duplicate() // this enables this thread to maintain its own position in a multi-threaded environment
                    myBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    myBufCapacity = myBuffer.capacity()
                } else {
                    myBaseAddr = metaData!!.baseAddrIPV6
                }
                myColumnSize = ipV6ColumnSize
                if (metaData!!.indexedIPV6) {
                    indexAddr = ipNo.shiftRight(112).toInt()
                    low = indexArrayIPV6[indexAddr][0].toLong()
                    high = indexArrayIPV6[indexAddr][1].toLong()
                }
            }
            if (ipNo.compareTo(maxIPRange) == 0) ipNo = ipNo.subtract(BigInteger.ONE)
            while (low <= high) {
                mid = ((low + high) / 2)
                rowOffset = myBaseAddr + mid * myColumnSize
                rowOffset2 = rowOffset + myColumnSize
                if (useMemoryMappedFile) {
                    overCapacity = rowOffset2 >= myBufCapacity
                }
                ipFrom = read32Or128(rowOffset, myIPType, myBuffer, fileHandle)
                ipTo = if (overCapacity) BigInteger.ZERO else read32Or128(rowOffset2, myIPType, myBuffer, fileHandle)
                if (ipNo >= ipFrom && ipNo < ipTo) {
                    var firstCol = 4 // IP From is 4 bytes
                    if (myIPType == 6) { // IPv6
                        firstCol = 16 // IPv6 is 16 bytes
                    }

                    // read the row here after the IP From column (remaining columns are all 4 bytes)
                    val rowLen = myColumnSize - firstCol
                    val row = readRow(rowOffset + firstCol, rowLen.toLong(), myBuffer, fileHandle)
                    if (useMemoryMappedFile) {
                        myDataBuffer = mapDataBuffer!!.duplicate() // this is to enable reading of a range of bytes in multi-threaded environment
                        myDataBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    }
                    if (countryEnabled) {
                        position = read32Row(row, countryPositionOffset).toLong()
                        record.countryShort = readStr(position, myDataBuffer, fileHandle)
                        position += 3
                        record.countryLong = readStr(position, myDataBuffer, fileHandle)
                    } else {
                        record.countryShort = IPResult.NOT_SUPPORTED
                        record.countryLong = IPResult.NOT_SUPPORTED
                    }
                    if (regionEnabled) {
                        position = read32Row(row, regionPositionOffset).toLong()
                        record.region = readStr(position, myDataBuffer, fileHandle)
                    } else {
                        record.region = IPResult.NOT_SUPPORTED
                    }
                    if (cityEnabled) {
                        position = read32Row(row, cityPositionOffset).toLong()
                        record.city = readStr(position, myDataBuffer, fileHandle)
                    } else {
                        record.city = IPResult.NOT_SUPPORTED
                    }
                    if (iSPEnabled) {
                        position = read32Row(row, iSPPositionOffset).toLong()
                        record.iSP = readStr(position, myDataBuffer, fileHandle)
                    } else {
                        record.iSP = IPResult.NOT_SUPPORTED
                    }
                    if (latitudeEnabled) {
                        record.latitude = setDecimalPlaces(readFloatRow(row, latitudePositionOffset)).toFloat()
                    } else {
                        record.latitude = null
                    }
                    if (longitudeEnabled) {
                        record.longitude = setDecimalPlaces(readFloatRow(row, longitudePositionOffset)).toFloat()
                    } else {
                        record.longitude = null
                    }
                    if (domainEnabled) {
                        position = read32Row(row, domainPositionOffset).toLong()
                        record.domain = readStr(position, myDataBuffer, fileHandle)
                    } else {
                        record.domain = IPResult.NOT_SUPPORTED
                    }
                    if (zIPCodeEnabled) {
                        position = read32Row(row, zIPCodePositionOffset).toLong()
                        record.zIPCode = readStr(position, myDataBuffer, fileHandle)
                    } else {
                        record.zIPCode = IPResult.NOT_SUPPORTED
                    }
                    if (timeZoneEnabled) {
                        position = read32Row(row, timeZonePositionOffset).toLong()
                        record.timeZone = readStr(position, myDataBuffer, fileHandle)
                    } else {
                        record.timeZone = IPResult.NOT_SUPPORTED
                    }
                    if (netSpeedEnabled) {
                        position = read32Row(row, netSpeedPositionOffset).toLong()
                        record.netSpeed = readStr(position, myDataBuffer, fileHandle)
                    } else {
                        record.netSpeed = IPResult.NOT_SUPPORTED
                    }
                    if (iDDCodeEnabled) {
                        position = read32Row(row, iDDCodePositionOffset).toLong()
                        record.iDDCode = readStr(position, myDataBuffer, fileHandle)
                    } else {
                        record.iDDCode = IPResult.NOT_SUPPORTED
                    }
                    if (areaCodeEnabled) {
                        position = read32Row(row, areaCodePositionOffset).toLong()
                        record.areaCode = readStr(position, myDataBuffer, fileHandle)
                    } else {
                        record.areaCode = IPResult.NOT_SUPPORTED
                    }
                    if (weatherStationCodeEnabled) {
                        position = read32Row(row, weatherStationCodePositionOffset).toLong()
                        record.weatherStationCode = readStr(position, myDataBuffer, fileHandle)
                    } else {
                        record.weatherStationCode = IPResult.NOT_SUPPORTED
                    }
                    if (weatherStationNameEnabled) {
                        position = read32Row(row, weatherStationNamePositionOffset).toLong()
                        record.weatherStationName = readStr(position, myDataBuffer, fileHandle)
                    } else {
                        record.weatherStationName = IPResult.NOT_SUPPORTED
                    }
                    if (mCCEnabled) {
                        position = read32Row(row, mCCPositionOffset).toLong()
                        record.mCC = readStr(position, myDataBuffer, fileHandle)
                    } else {
                        record.mCC = IPResult.NOT_SUPPORTED
                    }
                    if (mNCEnabled) {
                        position = read32Row(row, mNCPositionOffset).toLong()
                        record.mNC = readStr(position, myDataBuffer, fileHandle)
                    } else {
                        record.mNC = IPResult.NOT_SUPPORTED
                    }
                    if (mobileBrandEnabled) {
                        position = read32Row(row, mobileBrandPositionOffset).toLong()
                        record.mobileBrand = readStr(position, myDataBuffer, fileHandle)
                    } else {
                        record.mobileBrand = IPResult.NOT_SUPPORTED
                    }
                    if (elevationEnabled) {
                        position = read32Row(row, elevationPositionOffset).toLong()
                        record.elevation = convertFloat(readStr(position, myDataBuffer, fileHandle)) // due to value being stored as a string but output as float
                    } else {
                        record.elevation = 0.0f
                    }
                    if (usageTypeEnabled) {
                        position = read32Row(row, usageTypePositionOffset).toLong()
                        record.usageType = readStr(position, myDataBuffer, fileHandle)
                    } else {
                        record.usageType = IPResult.NOT_SUPPORTED
                    }
                    if (addressTypeEnabled) {
                        position = read32Row(row, addressTypePositionOffset).toLong()
                        record.addressType = readStr(position, myDataBuffer, fileHandle)
                    } else {
                        record.addressType = IPResult.NOT_SUPPORTED
                    }
                    if (categoryEnabled) {
                        position = read32Row(row, categoryPositionOffset).toLong()
                        record.category = readStr(position, myDataBuffer, fileHandle)
                    } else {
                        record.category = IPResult.NOT_SUPPORTED
                    }
                    record.status = "OK"
                    break
                } else {
                    if (ipNo < ipFrom) {
                        high = mid - 1
                    } else {
                        low = mid + 1
                    }
                }
            }
            return record
        } finally {
            fileHandle?.close()
        }
    }

    private fun expandIPV6(myIP: String, myIPType: Int): Array<String> {
        val tmp = "0000:0000:0000:0000:0000:"
        val padMe = "0000"
        val hexOffset: Long = 0xFF
        var myIP2 = myIP.toUpperCase()
        var retType = myIPType.toString()

        // expand ipv4-mapped ipv6
        if (myIPType == 4) {
            if (pattern4.matcher(myIP2).matches()) {
                myIP2 = myIP2.replace("::".toRegex(), tmp)
            } else {
                val mat = pattern5.matcher(myIP2)
                if (mat.matches()) {
                    val myMatch = mat.group(1)
                    val myArr =
                            myMatch.replace("^:+".toRegex(), "").replace(":+$".toRegex(), "").split(":".toRegex())
                                    .toTypedArray()
                    val len = myArr.size
                    val bf = StringBuffer(32)
                    for (x in 0 until len) {
                        val unPadded = myArr[x]
                        bf.append(padMe.substring(unPadded.length) + unPadded) // safe padding for JDK 1.4
                    }
                    var myLong: Long = BigInteger(bf.toString(), 16).toLong()
                    val b = longArrayOf(0, 0, 0, 0) // using long in place of bytes due to 2's complement signed issue
                    for (x in 0..3) {
                        b[x] = myLong and hexOffset
                        myLong = myLong shr 8
                    }
                    myIP2 = myIP2.replace(myMatch + "$".toRegex(), ":" + b[3] + "." + b[2] + "." + b[1] + "." + b[0])
                    myIP2 = myIP2.replace("::".toRegex(), tmp)
                }
            }
        } else if (myIPType == 6) {
            if (myIP2 == "::") {
                myIP2 += "0.0.0.0"
                myIP2 = myIP2.replace("::".toRegex(), tmp + "FFFF:")
                retType = "4"
            } else {
                // same regex as myIPType 4 but different scenario
                val mat = pattern4.matcher(myIP2)
                if (mat.matches()) {
                    val v6Part = mat.group(1)
                    val v4Part = mat.group(2)
                    val v4Arr = v4Part.split("\\.".toRegex()).toTypedArray()
                    val v4IntArr = IntArray(4)
                    var len = v4IntArr.size
                    for (x in 0 until len) {
                        v4IntArr[x] = v4Arr[x].toInt()
                    }
                    val part1 = (v4IntArr[0] shl 8) + v4IntArr[1]
                    val part2 = (v4IntArr[2] shl 8) + v4IntArr[3]
                    val part1Hex = Integer.toHexString(part1)
                    val part2Hex = Integer.toHexString(part2)
                    val bf = StringBuffer(v6Part.length + 9)
                    bf.append(v6Part)
                    bf.append(padMe.substring(part1Hex.length))
                    bf.append(part1Hex)
                    bf.append(":")
                    bf.append(padMe.substring(part2Hex.length))
                    bf.append(part2Hex)
                    myIP2 = bf.toString().toUpperCase()
                    val myArr = myIP2.split("::".toRegex()).toTypedArray()
                    val leftSide = myArr[0].split(":".toRegex()).toTypedArray()
                    val bf2 = StringBuffer(40)
                    val bf3 = StringBuffer(40)
                    val bf4 = StringBuffer(40)
                    len = leftSide.size
                    var totalSegments = 0
                    for (x in 0 until len) {
                        if (leftSide[x].isNotEmpty()) {
                            totalSegments++
                            bf2.append(padMe.substring(leftSide[x].length))
                            bf2.append(leftSide[x])
                            bf2.append(":")
                        }
                    }
                    if (myArr.size > 1) {
                        val rightSide = myArr[1].split(":".toRegex()).toTypedArray()
                        len = rightSide.size
                        for (x in 0 until len) {
                            if (rightSide[x].isNotEmpty()) {
                                totalSegments++
                                bf3.append(padMe.substring(rightSide[x].length))
                                bf3.append(rightSide[x])
                                bf3.append(":")
                            }
                        }
                    }
                    val totalSegmentsLeft = 8 - totalSegments
                    if (totalSegmentsLeft == 6) {
                        for (x in 1 until totalSegmentsLeft) {
                            bf4.append(padMe)
                            bf4.append(":")
                        }
                        bf4.append("FFFF:")
                        bf4.append(v4Part)
                        retType = "4"
                        myIP2 = bf4.toString()
                    } else {
                        for (x in 0 until totalSegmentsLeft) {
                            bf4.append(padMe)
                            bf4.append(":")
                        }
                        bf2.append(bf4).append(bf3)
                        myIP2 = bf2.toString().replace(":$".toRegex(), "")
                    }
                } else {
                    // expand IPv4-compatible IPv6
                    val mat2 = pattern6.matcher(myIP2)
                    if (mat2.matches()) {
                        val myMatch = mat2.group(1)
                        val myArr = myMatch.replace("^:+".toRegex(), "").replace(":+$".toRegex(), "").split(":".toRegex()).toTypedArray()
                        val len = myArr.size
                        val bf = StringBuffer(32)
                        for (x in 0 until len) {
                            val unPadded = myArr[x]
                            bf.append(padMe.substring(unPadded.length) + unPadded) // safe padding for JDK 1.4
                        }
                        var myLong: Long = BigInteger(bf.toString(), 16).toLong()
                        val b = longArrayOf(0, 0, 0, 0) // using long in place of bytes due to 2's complement signed issue
                        for (x in 0..3) {
                            b[x] = myLong and hexOffset
                            myLong = myLong shr 8
                        }
                        myIP2 = myIP2.replace(myMatch + "$".toRegex(), ":" + b[3] + "." + b[2] + "." + b[1] + "." + b[0])
                        myIP2 = myIP2.replace("::".toRegex(), tmp + "FFFF:")
                        retType = "4"
                    } else {
                        // should be normal IPv6 case
                        val myArr = myIP2.split("::".toRegex()).toTypedArray()
                        val leftSide = myArr[0].split(":".toRegex()).toTypedArray()
                        val bf2 = StringBuffer(40)
                        val bf3 = StringBuffer(40)
                        val bf4 = StringBuffer(40)
                        var len = leftSide.size
                        var totalSegments = 0
                        for (x in 0 until len) {
                            if (leftSide[x].isNotEmpty()) {
                                totalSegments++
                                bf2.append(padMe.substring(leftSide[x].length))
                                bf2.append(leftSide[x])
                                bf2.append(":")
                            }
                        }
                        if (myArr.size > 1) {
                            val rightSide = myArr[1].split(":".toRegex()).toTypedArray()
                            len = rightSide.size
                            for (x in 0 until len) {
                                if (rightSide[x].isNotEmpty()) {
                                    totalSegments++
                                    bf3.append(padMe.substring(rightSide[x].length))
                                    bf3.append(rightSide[x])
                                    bf3.append(":")
                                }
                            }
                        }
                        val totalSegmentsLeft = 8 - totalSegments
                        for (x in 0 until totalSegmentsLeft) {
                            bf4.append(padMe)
                            bf4.append(":")
                        }
                        bf2.append(bf4).append(bf3)
                        myIP2 = bf2.toString().replace(":$".toRegex(), "")
                    }
                }
            }
        }
        return arrayOf(myIP2, retType)
    }

    private fun convertFloat(myStr: String?): Float {
        return try {
            myStr!!.toFloat()
        } catch (e: NumberFormatException) {
            0.0f
        }
    }

    private fun reverse(array: ByteArray?) {
        if (array == null) {
            return
        }
        var i = 0
        var j = array.size - 1
        var tmp: Byte
        while (j > i) {
            tmp = array[j]
            array[j] = array[i]
            array[i] = tmp
            j--
            i++
        }
    }

    @Throws(IOException::class)
    private fun readRow(
            position: Long,
            myLen: Long,
            myBuffer: ByteBuffer?,
            fileHandle: RandomAccessFile?
    ): ByteArray {
        val row = ByteArray(myLen.toInt())
        if (useMemoryMappedFile) {
            myBuffer!!.position(position.toInt())
            myBuffer[row, 0, myLen.toInt()]
        } else {
            fileHandle!!.seek(position - 1)
            fileHandle.read(row, 0, myLen.toInt())
        }
        return row
    }

    @Throws(IOException::class)
    private fun read32Or128(
            position: Long,
            myIPType: Int,
            myBuffer: ByteBuffer?,
            fileHandle: RandomAccessFile?
    ): BigInteger {
        if (myIPType == 4) {
            return read32(position, myBuffer, fileHandle)
        } else if (myIPType == 6) {
            return read128(position, myBuffer, fileHandle) // only IPv6 will run this
        }
        return BigInteger.ZERO
    }

    @Throws(IOException::class)
    private fun read128(position: Long, myBuffer: ByteBuffer?, fileHandle: RandomAccessFile?): BigInteger {
        val retVal: BigInteger
        val bSize = 16
        val buf = ByteArray(bSize)
        if (useMemoryMappedFile) {
            myBuffer!!.position(position.toInt())
            myBuffer[buf, 0, bSize]
        } else {
            fileHandle!!.seek(position - 1)
            fileHandle.read(buf, 0, bSize)
        }
        reverse(buf)
        retVal = BigInteger(1, buf)
        return retVal
    }

    @Throws(IOException::class)
    private fun read32Row(row: ByteArray, from: Int): BigInteger {
        val len = 4 // 4 bytes
        val buf = ByteArray(len)
        System.arraycopy(row, from, buf, 0, len)
        reverse(buf)
        return BigInteger(1, buf)
    }

    @Throws(IOException::class)
    private fun read32(position: Long, myBuffer: ByteBuffer?, fileHandle: RandomAccessFile?): BigInteger {
        return if (useMemoryMappedFile) {
            // simulate unsigned int by using long
            BigInteger.valueOf(myBuffer!!.getInt(position.toInt()).toLong() and 0xffffffffL) // use absolute offset to be thread-safe
        } else {
            val bSize = 4
            fileHandle!!.seek(position - 1)
            val buf = ByteArray(bSize)
            fileHandle.read(buf, 0, bSize)
            reverse(buf)
            BigInteger(1, buf)
        }
    }

    @Throws(IOException::class)
    private fun readStr(
            position: Long,
            myDataBuffer: ByteBuffer?,
            fileHandle: RandomAccessFile?
    ): String? {
        var pos = position
        val size: Int
        val buf: ByteArray
        if (useMemoryMappedFile) {
            pos -= mapDataOffset // position stored in BIN file is for full file, not just the mapped data segment, so need to minus
            size = mapDataBuffer!![pos.toInt()].toInt() // use absolute offset to be thread-safe (keep using the original buffer since is absolute position & just reading 1 byte)
            try {
                buf = ByteArray(size)
                myDataBuffer!!.position(pos.toInt() + 1)
                myDataBuffer[buf, 0, size]
            } catch (e: NegativeArraySizeException) {
                return null
            }
        } else {
            fileHandle!!.seek(position)
            size = fileHandle.read()
            try {
                buf = ByteArray(size)
                fileHandle.read(buf, 0, size)
            } catch (e: NegativeArraySizeException) {
                return null
            }
        }
        return String(buf)
    }

    private fun readFloatRow(row: ByteArray, from: Int): Float {
        val len = 4 // 4 bytes
        val buf = ByteArray(len)
        System.arraycopy(row, from, buf, 0, len)
        return java.lang.Float.intBitsToFloat(((buf[3].toInt() and 0xff) shl 24) or ((buf[2].toInt() and 0xff) shl 16) or ((buf[1].toInt() and 0xff) shl 8) or (buf[0].toInt() and 0xff)) // the AND is converting byte to unsigned byte in the form of an int
    }

    private fun setDecimalPlaces(myFloat: Float): String {
        val currentLocale = Locale.getDefault()
        val nf = NumberFormat.getNumberInstance(currentLocale)
        val df = nf as DecimalFormat
        df.applyPattern("###.######")
        return df.format(myFloat.toDouble()).replace(',', '.')
    }

    @Throws(UnknownHostException::class)
    private fun ip2No(ipString: String): Array<BigInteger> {
        val a1: BigInteger
        var a2: BigInteger
        var a3 = BigInteger("4")
        if (pattern.matcher(ipString).matches()) { // should be IPv4
            a1 = BigInteger("4")
            a2 = BigInteger(ipV4No(ipString).toString())
        } else if (pattern2.matcher(ipString).matches() || pattern3.matcher(ipString).matches()) {
            throw UnknownHostException()
        } else {
            a3 = BigInteger("6")
            val ia = InetAddress.getByName(ipString)
            val byteArr = ia.address
            var myIPType = "0" // BigInteger needs String in the constructor
            if (ia is Inet6Address) {
                myIPType = "6"
            } else if (ia is Inet4Address) { // this will run in cases of IPv4-mapped IPv6 addresses
                myIPType = "4"
            }
            a2 = BigInteger(1, byteArr) // confirmed correct for IPv6
            if (a2 in FROM_6TO4..TO_6TO4) {
                // 6to4 so need to remap to ipv4
                myIPType = "4"
                a2 = a2.shiftRight(80)
                a2 = a2.and(LAST_32BITS)
                a3 = BigInteger("4")
            } else if (a2 in FROM_TEREDO..TO_TEREDO) {
                // Teredo so need to remap to ipv4
                myIPType = "4"
                a2 = a2.not()
                a2 = a2.and(LAST_32BITS)
                a3 = BigInteger("4")
            }
            a1 = BigInteger(myIPType)
        }
        return arrayOf(a1, a2, a3)
    }

    private fun ipV4No(ipString: String): Long {
        val ipAddressInArray = ipString.split("\\.".toRegex()).toTypedArray()
        var result: Long = 0
        var ip: Long
        for (x in 3 downTo 0) {
            ip = ipAddressInArray[3 - x].toLong()
            result = result or (ip shl (x shl 3))
        }
        return result
    }

    companion object {
        private val pattern =
                Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$") // IPv4
        private val pattern2 = Pattern.compile(
                "^([0-9A-F]{1,4}:){6}(0[0-9]+\\.|.*?\\.0[0-9]+).*$",
                Pattern.CASE_INSENSITIVE
        )
        private val pattern3 = Pattern.compile("^[0-9]+$")
        private val pattern4 = Pattern.compile("^(.*:)(([0-9]+\\.){3}[0-9]+)$")
        private val pattern5 = Pattern.compile("^.*((:[0-9A-F]{1,4}){2})$")
        private val pattern6 =
                Pattern.compile("^[0:]+((:[0-9A-F]{1,4}){1,2})$", Pattern.CASE_INSENSITIVE)
        private val MAX_IPV4_RANGE = BigInteger("4294967295")
        private val MAX_IPV6_RANGE = BigInteger("340282366920938463463374607431768211455")
        private val FROM_6TO4 = BigInteger("42545680458834377588178886921629466624")
        private val TO_6TO4 = BigInteger("42550872755692912415807417417958686719")
        private val FROM_TEREDO = BigInteger("42540488161975842760550356425300246528")
        private val TO_TEREDO = BigInteger("42540488241204005274814694018844196863")
        private val LAST_32BITS = BigInteger("4294967295")
        private val COUNTRY_POSITION =
                intArrayOf(0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2)
        private val REGION_POSITION =
                intArrayOf(0, 0, 0, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3)
        private val CITY_POSITION =
                intArrayOf(0, 0, 0, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
        private val ISP_POSITION =
                intArrayOf(0, 0, 3, 0, 5, 0, 7, 5, 7, 0, 8, 0, 9, 0, 9, 0, 9, 0, 9, 7, 9, 0, 9, 7, 9, 9)
        private val LATITUDE_POSITION =
                intArrayOf(0, 0, 0, 0, 0, 5, 5, 0, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5)
        private val LONGITUDE_POSITION =
                intArrayOf(0, 0, 0, 0, 0, 6, 6, 0, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6)
        private val DOMAIN_POSITION =
                intArrayOf(0, 0, 0, 0, 0, 0, 0, 6, 8, 0, 9, 0, 10, 0, 10, 0, 10, 0, 10, 8, 10, 0, 10, 8, 10, 10)
        private val ZIPCODE_POSITION =
                intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 7, 7, 7, 0, 7, 7, 7, 0, 7, 0, 7, 7, 7, 0, 7, 7)
        private val TIMEZONE_POSITION =
                intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 8, 7, 8, 8, 8, 7, 8, 0, 8, 8, 8, 0, 8, 8)
        private val NETSPEED_POSITION =
                intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 11, 0, 11, 8, 11, 0, 11, 0, 11, 0, 11, 11)
        private val IDDCODE_POSITION =
                intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 12, 0, 12, 0, 12, 9, 12, 0, 12, 12)
        private val AREACODE_POSITION =
                intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 13, 0, 13, 0, 13, 10, 13, 0, 13, 13)
        private val WEATHERSTATIONCODE_POSITION =
                intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 14, 0, 14, 0, 14, 0, 14, 14)
        private val WEATHERSTATIONNAME_POSITION =
                intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 15, 0, 15, 0, 15, 0, 15, 15)
        private val MCC_POSITION =
                intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 16, 0, 16, 9, 16, 16)
        private val MNC_POSITION =
                intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 17, 0, 17, 10, 17, 17)
        private val MOBILEBRAND_POSITION =
                intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 11, 18, 0, 18, 11, 18, 18)
        private val ELEVATION_POSITION =
                intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 11, 19, 0, 19, 19)
        private val USAGETYPE_POSITION =
                intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12, 20, 20)
        private val ADDRESSTYPE_POSITION =
                intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 21)
        private val CATEGORY_POSITION =
                intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 22)
    }
}
