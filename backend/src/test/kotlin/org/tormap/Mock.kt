package org.tormap

import org.tormap.database.entity.RelayDetails
import java.time.LocalDate

fun relayDetailsMock(mockLetter: Char? = null) = RelayDetails(
    "2022",
    LocalDate.of(2022, 2,4),
    mockLetter?.toString(),
    null,
    "172.104.114.113",
    true,
    mockLetter?.toString() ?: "Hydra1",
    1073741824,
    1073741824,
    60223,
    null,
    null,
    mockLetter?.toString()?.repeat(40) ?: "0B5265EBA4BF8F0DFDA73F68928EC5BF7F88DEF1",
    true,
    null,
    null,
    null,
    null,
    false,
    false,
    null,
    null,
    false,
)

