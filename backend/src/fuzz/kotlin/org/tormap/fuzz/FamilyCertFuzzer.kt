package org.tormap.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import org.tormap.util.verifyFamilyCert

/**
 * Family certificates are binary data from relay descriptors, parsed by TorMap's own code.
 * The relay identity key is taken from the certificate itself and the publication time is 0, so inputs get past the
 * identity and expiry checks to the extensions and the signature.
 */
object FamilyCertFuzzer {
    private const val CERTIFIED_KEY_OFFSET = 7
    private const val KEY_LENGTH = 32

    @JvmStatic
    fun fuzzerTestOneInput(data: FuzzedDataProvider) {
        val certificate = data.consumeRemainingAsBytes()
        val relayIdentityKey =
            if (certificate.size >= CERTIFIED_KEY_OFFSET + KEY_LENGTH) {
                certificate.copyOfRange(CERTIFIED_KEY_OFFSET, CERTIFIED_KEY_OFFSET + KEY_LENGTH)
            } else {
                ByteArray(KEY_LENGTH)
            }
        verifyFamilyCert(certificate, relayIdentityKey, 0)
    }
}
