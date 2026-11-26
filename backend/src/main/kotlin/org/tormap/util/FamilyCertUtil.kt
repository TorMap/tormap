package org.tormap.util

import org.torproject.descriptor.ServerDescriptor
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Family keys of the families this relay proves membership in with its `family-cert` entries
 * ("happy families", Tor proposal 321, since Tor 0.4.9). Relays with the same family key belong to the same family.
 * Invalid certificates are skipped.
 *
 * See https://spec.torproject.org/dir-spec/server-descriptor-format.html and https://spec.torproject.org/cert-spec.html
 *
 * Tor calls these family IDs (microdescriptor `family-ids`). TorMap calls them family keys, so they are not confused
 * with its own numeric [org.tormap.database.entity.RelayDetails.familyId].
 *
 * @return sorted family keys like `ed25519:<unpadded base64 of the family public key>`
 */
fun ServerDescriptor.verifiedFamilyKeys(): List<String> {
    val masterKey =
        masterKeyEd25519?.let { decodeBase64OrNull(it) }?.takeIf { it.size == KEY_LENGTH } ?: return emptyList()
    return familyCerts
        .mapNotNull { cryptoBlock ->
            val certificate = decodeBase64OrNull(
                cryptoBlock.lines().filterNot { it.startsWith("-----") }.joinToString(""),
            ) ?: return@mapNotNull null
            verifyFamilyCert(certificate, masterKey, publishedMillis)
        }
        .map { FAMILY_KEY_PREFIX + Base64.getEncoder().withoutPadding().encodeToString(it) }
        .distinct()
        .sorted()
}

const val FAMILY_KEY_PREFIX = "ed25519:"

/**
 * Checks an Ed25519 family certificate as described in the cert-spec and the `family-cert` item of the dir-spec.
 * @param relayIdentityKey the relay's Ed25519 identity key (master-key-ed25519)
 * @param publishedMillis when the descriptor was published, the certificate must not be expired at that time
 * @return the family key that signed the certificate, or null if the certificate is not valid
 */
fun verifyFamilyCert(certificate: ByteArray, relayIdentityKey: ByteArray, publishedMillis: Long): ByteArray? {
    if (certificate.size < HEADER_LENGTH + SIGNATURE_LENGTH) return null
    val buffer = ByteBuffer.wrap(certificate)
    val version = buffer.get().toInt()
    val certType = buffer.get().toInt()
    val expirationHours = Integer.toUnsignedLong(buffer.getInt())
    val certifiedKeyType = buffer.get().toInt()
    val certifiedKey = ByteArray(KEY_LENGTH).also { buffer.get(it) }
    val extensionCount = buffer.get().toInt() and 0xFF
    if (
        version != 1 ||
        certType != CERT_TYPE_FAMILY_V_IDENTITY ||
        certifiedKeyType != CERTIFIED_KEY_TYPE_ED25519 ||
        !certifiedKey.contentEquals(relayIdentityKey) ||
        expirationHours * MILLIS_PER_HOUR < publishedMillis
    ) {
        return null
    }

    var signingKey: ByteArray? = null
    repeat(extensionCount) {
        if (buffer.remaining() < EXTENSION_HEADER_LENGTH + SIGNATURE_LENGTH) return null
        val length = buffer.getShort().toInt() and 0xFFFF
        val type = buffer.get().toInt()
        val flags = buffer.get().toInt()
        if (buffer.remaining() < length + SIGNATURE_LENGTH) return null
        val data = ByteArray(length).also { buffer.get(it) }
        when {
            type == EXTENSION_TYPE_SIGNED_WITH_ED25519_KEY && length == KEY_LENGTH -> signingKey = data

            type == EXTENSION_TYPE_SIGNED_WITH_ED25519_KEY -> return null

            // Unknown extensions that affect validation must not be ignored
            (flags and EXTENSION_FLAG_AFFECTS_VALIDATION) != 0 -> return null
        }
    }
    // The family key must be included in the certificate and the signature must follow the extensions directly
    val familyKey = signingKey ?: return null
    if (buffer.remaining() != SIGNATURE_LENGTH) return null
    val signedLength = certificate.size - SIGNATURE_LENGTH
    return familyKey.takeIf {
        verifyEd25519(
            publicKey = familyKey,
            message = certificate.copyOfRange(0, signedLength),
            signature = certificate.copyOfRange(signedLength, certificate.size),
        )
    }
}

private fun verifyEd25519(publicKey: ByteArray, message: ByteArray, signature: ByteArray) = try {
    val key = KeyFactory.getInstance("Ed25519").generatePublic(X509EncodedKeySpec(ED25519_X509_PREFIX + publicKey))
    Signature.getInstance("Ed25519").run {
        initVerify(key)
        update(message)
        verify(signature)
    }
} catch (_: GeneralSecurityException) {
    false
}

private fun decodeBase64OrNull(value: String) = try {
    Base64.getDecoder().decode(value.trim().trimEnd('=').let { it + "=".repeat((4 - it.length % 4) % 4) })
} catch (_: IllegalArgumentException) {
    null
}

private const val CERT_TYPE_FAMILY_V_IDENTITY = 0x0C
private const val CERTIFIED_KEY_TYPE_ED25519 = 0x01
private const val EXTENSION_TYPE_SIGNED_WITH_ED25519_KEY = 0x04
private const val EXTENSION_FLAG_AFFECTS_VALIDATION = 0x01
private const val KEY_LENGTH = 32
private const val SIGNATURE_LENGTH = 64
private const val HEADER_LENGTH = 1 + 1 + 4 + 1 + KEY_LENGTH + 1
private const val EXTENSION_HEADER_LENGTH = 4
private const val MILLIS_PER_HOUR = 3_600_000L

/** DER prefix of an X.509 SubjectPublicKeyInfo for an Ed25519 key (RFC 8410) */
private val ED25519_X509_PREFIX = byteArrayOf(0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00)
