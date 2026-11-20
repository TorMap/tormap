package org.tormap.cli

/**
 * The API v1 fields the CLI reads or prints, per schema of api/openapi-v1.json. ContractTest checks that each of
 * them exists in the spec, so a renamed field breaks the build instead of printing empty columns.
 */
object Fields {
    /** RelayV1Dto, the relays of a day */
    val RELAY = listOf("fingerprint", "nickname", "country", "flags", "consensusWeight", "autonomousSystemNumber", "familyId", "lat", "long")

    /** RelayDetailsV1Dto */
    val RELAY_DETAILS = listOf(
        "fingerprint", "nickname", "month", "day", "address", "autonomousSystemNumber", "autonomousSystemName",
        "platform", "protocols", "contact", "uptime", "bandwidthRate", "bandwidthBurst", "bandwidthObserved",
        "hibernating", "allowSingleHopExits", "cachesExtraInfo", "hiddenServiceDir", "tunnelledDirServer",
        "linkProtocolVersions", "circuitProtocolVersions", "familyEntries", "familyId", "confirmedFamilyMembers",
        "verifiedHostNames", "unverifiedHostNames",
    )

    /** RelayIdentifiersV1Dto, members of a family */
    val FAMILY_MEMBER = listOf("fingerprint", "nickname")

    /** AutonomousSystemDto */
    val AUTONOMOUS_SYSTEM = listOf("number", "name")

    /** DailyRelayCountDto */
    val DAILY_COUNT = listOf("day", "total", "exit", "guard", "other")

    /** CountryUsageDto */
    val COUNTRY_USAGE = listOf("country", "users", "lower", "upper", "frac")

    /** The relay columns shown in a terminal table; CSV and JSON contain all fields */
    val RELAY_TABLE = listOf("fingerprint", "nickname", "country", "consensusWeight", "autonomousSystemNumber", "flags")
}
