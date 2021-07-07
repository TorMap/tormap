# com.ip2location.IP2Location IP Geolocation Kotlin Module

This IP Geolocation Kotlin module allows user to query an IP address for info such as the visitor’s country, region, city, ISP or company name. In addition, users can also determine extra useful geolocation information such as latitude, longitude, ZIP code, domain name, time zone, connection speed, IDD code, area code, weather station code, weather station name, MCC, MNC, mobile brand name, elevation, usage type, address type and IAB category. It lookup the IP address from **com.ip2location.IP2Location BIN Data** file. This data file can be downloaded at

* Free com.ip2location.IP2Location IP Geolocation BIN Data: https://lite.ip2location.com
* Commercial com.ip2location.IP2Location IP Geolocation BIN Data: https://www.ip2location.com/database/ip2location

As an alternative, this IP Geolocation Kotlin module can also call the com.ip2location.IP2Location Web Service. This requires an API key. If you don't have an existing API key, you can subscribe for one at the below:

https://www.ip2location.com/web-service/ip2location

## Requirements ##
Intellij IDEA: https://www.jetbrains.com/idea/

## QUERY USING THE BIN FILE

## Functions
Below are the functions supported in this module.

|Function Name|Description|
|---|---|
|open(DBPath: String, UseMMF: Boolean)|Initialize the component with the BIN file path and whether to use MemoryMappedFile.|
|open(DBPath: String)|Initialize the component with the BIN file path.|
|ipQuery(IPAddress: String?)|Query IP address. This function returns results in the com.ip2location.IPResult object.|
|close()|Destroys the mapped bytes.|

## Result properties
Below are the result properties.

|Property Name|Description|
|---|---|
|countryShort|Two-character country code based on ISO 3166.|
|countryLong|Country name based on ISO 3166.|
|region|Region or state name.|
|city|City name.|
|latitude|City level latitude.|
|longitude|City level longitude.|
|zIPCode|ZIP code or postal code.|
|timeZone|Time zone in UTC (Coordinated Universal Time).|
|iSP|Internet Service Provider (ISP) name.|
|domain|Domain name associated to IP address range.|
|netSpeed|Internet connection speed <ul><li>(DIAL) Dial-up</li><li>(DSL) DSL/Cable</li><li>(COMP) Company/T1</li></ul>|
|iDDCode|The IDD prefix to call the city from another country.|
|areaCode|A varying length number assigned to geographic areas for call between cities.|
|weatherStationCode|Special code to identify the nearest weather observation station.|
|weatherStationName|Name of the nearest weather observation station.|
|mCC|Mobile country code.|
|mNC|Mobile network code.|
|mobileBrand|Mobile carrier brand.|
|elevation|Average height of city above sea level in meters (m).|
|usageType|Usage type classification of ISP or company:<ul><li>(COM) Commercial</li><li>(ORG) Organization</li><li>(GOV) Government</li><li>(MIL) Military</li><li>(EDU) University/College/School</li><li>(LIB) Library</li><li>(CDN) Content Delivery Network</li><li>(ISP) Fixed Line ISP</li><li>(MOB) Mobile ISP</li><li>(DCH) Data Center/Web Hosting/Transit</li><li>(SES) Search Engine Spider</li><li>(RSV) Reserved</li></ul>|
|addressType|IP address types as defined in Internet Protocol version 4 (IPv4) and Internet Protocol version 6 (IPv6).<ul><li>(A) Anycast - One to the closest</li><li>(U) Unicast - One to one</li><li>(M) Multicast - One to multiple</li><li>(B) Broadcast - One to all</li></ul>|
|category|The domain category is based on [IAB Tech Lab Content Taxonomy](https://www.ip2location.com/free/iab-categories). These categories are comprised of Tier-1 and Tier-2 (if available) level categories widely used in services like advertising, Internet security and filtering appliances.|
|status|Status code of query.|

## Status codes
Below are the status codes.
|Code|Description|
|---|---|
|OK|The query has been successfully performed.|
|EMPTY_IP_ADDRESS|The IP address is empty.|
|INVALID_IP_ADDRESS|The format of the IP address is wrong.|
|MISSING_FILE|The BIN file path is wrong or the BIN file is unreadable.|
|IP_ADDRESS_NOT_FOUND|The IP address does not exists in the BIN file.|
|IPV6_NOT_SUPPORTED|The BIN file does not contain IPv6 data.|

## Usage

```kotlin
import kotlin.jvm.JvmStatic
import java.lang.Exception

object com.ip2location.Main {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val strIPAddress = "8.8.8.8"

            // querying with the BIN file
            val dbPath = "/usr/data/IP-COUNTRY-REGION-CITY-LATITUDE-LONGITUDE-ZIPCODE-TIMEZONE-ISP-DOMAIN-NETSPEED-AREACODE-WEATHER-MOBILE-ELEVATION-USAGETYPE-ADDRESSTYPE-CATEGORY.BIN"
            val useMMF = true

            var loc = com.ip2location.IP2Location()
            loc.open(dbPath, useMMF)

            var rec = loc.ipQuery(strIPAddress)

            when (rec.status) {
                "OK" -> println(rec)
                "EMPTY_IP_ADDRESS" -> println("IP address cannot be blank.")
                "INVALID_IP_ADDRESS" -> println("Invalid IP address.")
                "MISSING_FILE" -> println("Invalid database path.")
                "IPV6_NOT_SUPPORTED" -> println("This BIN does not contain IPv6 data.")
                else -> println("Unknown error." + rec.status)
            }
            loc.close()

        } catch (e: Exception) {
            println(e)
            e.printStackTrace(System.out)
        }
    }
}
```

## QUERY USING THE IP2LOCATION IP GEOLOCATION WEB SERVICE

## Functions
Below are the functions supported in this module.

|Function Name|Description|
|---|---|
|open(aPIKey: String, packageName: String, useSSL: Boolean = true)|Initialize component with the API key and package (WS1 to WS25).|
|ipQuery(ipAddress: String?)|Query IP address. This function returns a JsonObject.|
|ipQuery(ipAddress: String?, language: String?)|Query IP address and translation language. This function returns a JsonObject.|
|ipQuery(ipAddress: String?, addOns: Array<String>, language: String?)|Query IP address and Addons. This function returns a JsonObject.|
|getCredit()|This function returns the web service credit balance in a JsonObject.|

Below are the Addons supported in this module.

|Addon Name|Description|
|---|---|
|continent|Returns continent code, name, hemispheres and translations.|
|country|Returns country codes, country name, flag, capital, total area, population, currency info, language info, IDD, TLD and translations.|
|region|Returns region code, name and translations.|
|city|Returns city name and translations.|
|geotargeting|Returns metro code based on the ZIP/postal code.|
|country_groupings|Returns group acronyms and names.|
|time_zone_info|Returns time zones, DST, GMT offset, sunrise and sunset.|

## Result fields
Below are the result fields.

|Name|
|---|
|<ul><li>country_code</li><li>country_name</li><li>region_name</li><li>city_name</li><li>latitude</li><li>longitude</li><li>zip_code</li><li>time_zone</li><li>isp</li><li>domain</li><li>net_speed</li><li>idd_code</li><li>area_code</li><li>weather_station_code</li><li>weather_station_name</li><li>mcc</li><li>mnc</li><li>mobile_brand</li><li>elevation</li><li>usage_type</li><li>address_type</li><li>category</li><li>category_name</li><li>continent<ul><li>name</li><li>code</li><li>hemisphere</li><li>translations</li></ul></li><li>country<ul><li>name</li><li>alpha3_code</li><li>numeric_code</li><li>demonym</li><li>flag</li><li>capital</li><li>total_area</li><li>population</li><li>currency<ul><li>code</li><li>name</li><li>symbol</li></ul></li><li>language<ul><li>code</li><li>name</li></ul></li><li>idd_code</li><li>tld</li><li>translations</li></ul></li><li>region<ul><li>name</li><li>code</li><li>translations</li></ul></li><li>city<ul><li>name</li><li>translations</li></ul></li><li>geotargeting<ul><li>metro</li></ul></li><li>country_groupings</li><li>time_zone_info<ul><li>olson</li><li>current_time</li><li>gmt_offset</li><li>is_dst</li><li>sunrise</li><li>sunset</li></ul></li><ul>|

## Usage

```kotlin
import kotlin.jvm.JvmStatic
import com.google.gson.JsonObject
import java.lang.Exception

object com.ip2location.Main {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val strIPAddress = "8.8.8.8"

            // querying with the web service
            val ws = IP2LocationWebService()
            val strAPIKey = "XXXXXXXXXX" // replace this with your com.ip2location.IP2Location Web Service API key
            val strPackage = "WS25"
            val addOn = arrayOf("continent", "country", "region", "city", "geotargeting", "country_groupings", "time_zone_info")
            val strLang = "es"
            val boolSSL = true
            ws.open(strAPIKey, strPackage, boolSSL)
            var myResult: JsonObject = ws.ipQuery(strIPAddress, addOn, strLang)
            if (myResult["response"] == null) {
                // standard results
                println("country_code: " + if (myResult["country_code"] != null) myResult["country_code"].asString else "")
                println("country_name: " + if (myResult["country_name"] != null) myResult["country_name"].asString else "")
                println("region_name: " + if (myResult["region_name"] != null) myResult["region_name"].asString else "")
                println("city_name: " + if (myResult["city_name"] != null) myResult["city_name"].asString else "")
                println("latitude: " + if (myResult["latitude"] != null) myResult["latitude"].asString else "")
                println("longitude: " + if (myResult["longitude"] != null) myResult["longitude"].asString else "")
                println("zip_code: " + if (myResult["zip_code"] != null) myResult["zip_code"].asString else "")
                println("time_zone: " + if (myResult["time_zone"] != null) myResult["time_zone"].asString else "")
                println("isp: " + if (myResult["isp"] != null) myResult["isp"].asString else "")
                println("domain: " + if (myResult["domain"] != null) myResult["domain"].asString else "")
                println("net_speed: " + if (myResult["net_speed"] != null) myResult["net_speed"].asString else "")
                println("idd_code: " + if (myResult["idd_code"] != null) myResult["idd_code"].asString else "")
                println("area_code: " + if (myResult["area_code"] != null) myResult["area_code"].asString else "")
                println("weather_station_code: " + if (myResult["weather_station_code"] != null) myResult["weather_station_code"].asString else "")
                println("weather_station_name: " + if (myResult["weather_station_name"] != null) myResult["weather_station_name"].asString else "")
                println("mcc: " + if (myResult["mcc"] != null) myResult["mcc"].asString else "")
                println("mnc: " + if (myResult["mnc"] != null) myResult["mnc"].asString else "")
                println("mobile_brand: " + if (myResult["mobile_brand"] != null) myResult["mobile_brand"].asString else "")
                println("elevation: " + if (myResult["elevation"] != null) myResult["elevation"].asString else "")
                println("usage_type: " + if (myResult["usage_type"] != null) myResult["usage_type"].asString else "")
                println("address_type: " + if (myResult["address_type"] != null) myResult["address_type"].asString else "")
                println("category: " + if (myResult["category"] != null) myResult["category"].asString else "")
                println("category_name: " + if (myResult["category_name"] != null) myResult["category_name"].asString else "")
                println("credits_consumed: " + if (myResult["credits_consumed"] != null) myResult["credits_consumed"].asString else "")

                // continent addon
                if (myResult["continent"] != null) {
                    val continentObj = myResult.getAsJsonObject("continent")
                    println("continent => name: " + continentObj["name"].asString)
                    println("continent => code: " + continentObj["code"].asString)
                    val myArr = continentObj.getAsJsonArray("hemisphere")
                    println("continent => hemisphere: $myArr")
                    println("continent => translations: " + continentObj.getAsJsonObject("translations")[strLang].asString)
                }

                // country addon
                if (myResult["country"] != null) {
                    val countryObj = myResult.getAsJsonObject("country")
                    println("country => name: " + countryObj["name"].asString)
                    println("country => alpha3_code: " + countryObj["alpha3_code"].asString)
                    println("country => numeric_code: " + countryObj["numeric_code"].asString)
                    println("country => demonym: " + countryObj["demonym"].asString)
                    println("country => flag: " + countryObj["flag"].asString)
                    println("country => capital: " + countryObj["capital"].asString)
                    println("country => total_area: " + countryObj["total_area"].asString)
                    println("country => population: " + countryObj["population"].asString)
                    println("country => idd_code: " + countryObj["idd_code"].asString)
                    println("country => tld: " + countryObj["tld"].asString)
                    println("country => translations: " + countryObj.getAsJsonObject("translations")[strLang].asString)
                    val currencyObj = countryObj.getAsJsonObject("currency")
                    println("country => currency => code: " + currencyObj["code"].asString)
                    println("country => currency => name: " + currencyObj["name"].asString)
                    println("country => currency => symbol: " + currencyObj["symbol"].asString)
                    val languageObj = countryObj.getAsJsonObject("language")
                    println("country => language => code: " + languageObj["code"].asString)
                    println("country => language => name: " + languageObj["name"].asString)
                }

                // region addon
                if (myResult["region"] != null) {
                    val regionObj = myResult.getAsJsonObject("region")
                    println("region => name: " + regionObj["name"].asString)
                    println("region => code: " + regionObj["code"].asString)
                    println("region => translations: " + regionObj.getAsJsonObject("translations")[strLang].asString)
                }

                // city addon
                if (myResult["city"] != null) {
                    val cityObj = myResult.getAsJsonObject("city")
                    println("city => name: " + cityObj["name"].asString)
                    println("city => translations: " + cityObj.getAsJsonArray("translations").toString())
                }

                // geotargeting addon
                if (myResult["geotargeting"] != null) {
                    val geoObj = myResult.getAsJsonObject("geotargeting")
                    println("geotargeting => metro: " + geoObj["metro"].asString)
                }

                // country_groupings addon
                if (myResult["country_groupings"] != null) {
                    val myArr = myResult.getAsJsonArray("country_groupings")
                    if (myArr.size() > 0) {
                        for (x in 0 until myArr.size()) {
                            println("country_groupings => #" + x + " => acronym: " + myArr[x].asJsonObject["acronym"].asString)
                            println("country_groupings => #" + x + " => name: " + myArr[x].asJsonObject["name"].asString)
                        }
                    }
                }

                // time_zone_info addon
                if (myResult["time_zone_info"] != null) {
                    val tzObj = myResult.getAsJsonObject("time_zone_info")
                    println("time_zone_info => olson: " + tzObj["olson"].asString)
                    println("time_zone_info => current_time: " + tzObj["current_time"].asString)
                    println("time_zone_info => gmt_offset: " + tzObj["gmt_offset"].asString)
                    println("time_zone_info => is_dst: " + tzObj["is_dst"].asString)
                    println("time_zone_info => sunrise: " + tzObj["sunrise"].asString)
                    println("time_zone_info => sunset: " + tzObj["sunset"].asString)
                }
            } else {
                println("Error: " + myResult["response"].asString)
            }
            myResult = ws.getCredit()
            if (myResult["response"] != null) {
                println("Credit balance: " + myResult["response"].asString)
            }
        } catch (e: Exception) {
            println(e)
            e.printStackTrace(System.out)
        }
    }
}
```
