package org.tormap.cli

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.nio.file.Files
import java.nio.file.Path

/**
 * The CLI reads fields by name. This checks every name against api/openapi-v1.json, which the backend's
 * ApiV1OpenApiSpecTest keeps in sync with the server, so a renamed field fails here instead of printing empty columns.
 */
class ContractTest : StringSpec({
    val spec = Json.parseToJsonElement(Files.readString(Path.of(System.getProperty("tormap.apiSpec")))).jsonObject
    val schemas = spec["components"]!!.jsonObject["schemas"]!!.jsonObject
    val paths = spec["paths"]!!.jsonObject.keys

    fun missing(schema: String, fields: List<String>) = fields - schemas[schema]!!.jsonObject["properties"]!!.jsonObject.keys

    "every field the CLI reads exists in the API v1 spec" {
        missing("RelayV1Dto", Fields.RELAY + Fields.RELAY_TABLE).shouldBeEmpty()
        missing("RelayDetailsV1Dto", Fields.RELAY_DETAILS).shouldBeEmpty()
        missing("RelayIdentifiersV1Dto", Fields.FAMILY_MEMBER).shouldBeEmpty()
        missing("RelayMonthV1Dto", Fields.RELAY_MONTH + Fields.RELAY_MONTH_TABLE).shouldBeEmpty()
        missing("AutonomousSystemDto", Fields.AUTONOMOUS_SYSTEM).shouldBeEmpty()
        missing("DailyRelayCountDto", Fields.DAILY_COUNT).shouldBeEmpty()
        missing("CountryUsageDto", Fields.COUNTRY_USAGE).shouldBeEmpty()
    }

    "every path the CLI calls exists in the API v1 spec" {
        val used = listOf(
            "/v1/days", "/v1/days/{day}/relays", "/v1/relays/{fingerprint}", "/v1/relays/{fingerprint}/months", "/v1/families/{id}",
            "/v1/autonomous-systems/{month}", "/v1/statistics/daily-counts", "/v1/country-usage/{day}",
        )
        (used - paths).shouldBeEmpty()
    }
})
