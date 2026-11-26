package org.tormap.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import org.tormap.util.UserStatsCsvParser

/**
 * The Tor Metrics CSV of relay users per country is untrusted content. Invalid lines must be skipped, never fail.
 */
object UserStatsCsvFuzzer {
    @JvmStatic
    fun fuzzerTestOneInput(data: FuzzedDataProvider) {
        UserStatsCsvParser.parse(data.consumeRemainingAsString().lineSequence()).toList()
    }
}
