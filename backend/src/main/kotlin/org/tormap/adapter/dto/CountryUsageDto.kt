package org.tormap.adapter.dto

import java.time.LocalDate

/**
 * The estimated number of relay users of a country on a day, as imported from Tor Metrics
 */
class CountryUsageRow(
    val day: LocalDate,
    val countryCode: String,
    val users: Int,
    val lower: Int?,
    val upper: Int?,
    val frac: Int,
)

/**
 * The estimated number of relay users of a country on the requested day
 * @param country the ISO 3166-1 alpha-2 code in upper case
 * @param lower the lower bound of the estimate, null if Tor Metrics has none
 * @param upper the upper bound of the estimate, null if Tor Metrics has none
 * @param frac the percentage of the network that contributed to the estimate of the day, low values are less reliable
 */
class CountryUsageDto(
    val country: String,
    val users: Int,
    val lower: Int?,
    val upper: Int?,
    val frac: Int,
)
