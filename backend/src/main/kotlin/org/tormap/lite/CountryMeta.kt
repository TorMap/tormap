package org.tormap.lite

import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Names and map colors of countries from the frontend's country-meta.json (copied into the resources by the build)
 */
@Component
class CountryMeta(objectMapper: ObjectMapper) {
    private class Country(val name: String, val mapColor: Int)

    private val countries: Map<String, Country> = CountryMeta::class.java.getResourceAsStream(RESOURCE)
        .let { requireNotNull(it) { "Missing resource $RESOURCE" } }
        .use { objectMapper.readTree(it) }
        .associate { it.get("iso_a2").asString() to Country(it.get("name").asString(), it.get("mapcolor9")?.asInt() ?: 0) }

    /** The country name, or the code itself if it is unknown */
    fun name(isoCode: String): String = countries[isoCode]?.name ?: isoCode

    /** One of 9 colors (0 to 8, Natural Earth `mapcolor9`) so that neighboring countries differ, like getUniqueCountryColor() */
    fun mapColor(isoCode: String): Int = countries[isoCode]?.mapColor ?: 0

    companion object {
        private const val RESOURCE = "/lite/country-meta.json"
    }
}
