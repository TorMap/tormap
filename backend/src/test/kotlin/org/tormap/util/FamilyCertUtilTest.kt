package org.tormap.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.torproject.descriptor.DescriptorSourceFactory
import org.torproject.descriptor.ServerDescriptor
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.time.Instant
import java.util.Base64

class FamilyCertUtilTest : StringSpec({
    val published = Instant.parse("2026-11-17T12:00:00Z").toEpochMilli()
    val validUntilHours = (published / 3_600_000L + 24 * 30).toInt()
    val familyKeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    val relayKey = KeyPairGenerator.getInstance("Ed25519").generateKeyPair().rawPublicKey()

    class Extension(val type: Int, val flags: Int, val data: ByteArray)

    fun certificate(
        signer: KeyPair = familyKeyPair,
        certType: Int = 0x0C,
        expirationHours: Int = validUntilHours,
        certifiedKey: ByteArray = relayKey,
        extensions: List<Extension> = listOf(Extension(0x04, 0, signer.rawPublicKey())),
        trailingBytes: ByteArray = byteArrayOf(),
    ): ByteArray {
        val body = ByteArrayOutputStream().apply {
            write(1)
            write(certType)
            write(ByteBuffer.allocate(4).putInt(expirationHours).array())
            write(1)
            write(certifiedKey)
            write(extensions.size)
            extensions.forEach {
                write(ByteBuffer.allocate(2).putShort(it.data.size.toShort()).array())
                write(it.type)
                write(it.flags)
                write(it.data)
            }
        }.toByteArray()
        val signature = Signature.getInstance("Ed25519").run {
            initSign(signer.private)
            update(body)
            sign()
        }
        return body + signature + trailingBytes
    }

    "a valid family certificate returns the family key" {
        verifyFamilyCert(certificate(), relayKey, published) shouldBe familyKeyPair.rawPublicKey()
    }

    "an unknown extension that does not affect validation is ignored" {
        val extensions = listOf(Extension(0x04, 0, familyKeyPair.rawPublicKey()), Extension(0x42, 0, byteArrayOf(1, 2)))

        verifyFamilyCert(certificate(extensions = extensions), relayKey, published) shouldBe familyKeyPair.rawPublicKey()
    }

    mapOf(
        "wrong certificate type" to certificate(certType = 0x04),
        "expired before the descriptor was published" to certificate(expirationHours = (published / 3_600_000L - 1).toInt()),
        "certifies another relay" to certificate(certifiedKey = ByteArray(32) { 7 }),
        "no signing key extension" to certificate(extensions = emptyList()),
        "unknown extension affecting validation" to certificate(
            extensions = listOf(Extension(0x04, 0, familyKeyPair.rawPublicKey()), Extension(0x42, 1, byteArrayOf()))
        ),
        "signed by another key than the included one" to certificate(
            extensions = listOf(
                Extension(0x04, 0, KeyPairGenerator.getInstance("Ed25519").generateKeyPair().rawPublicKey())
            )
        ),
        "trailing bytes" to certificate(trailingBytes = byteArrayOf(0)),
        "truncated" to certificate().copyOfRange(0, 60),
        "tampered" to certificate().also { it[3] = (it[3] + 1).toByte() },
    ).forEach { (reason, certificate) ->
        "an invalid family certificate is rejected ($reason)" {
            verifyFamilyCert(certificate, relayKey, published).shouldBeNull()
        }
    }

    fun parseServerDescriptor(text: String) = DescriptorSourceFactory.createDescriptorParser()
        .parseDescriptors(text.toByteArray(), null, "server-descriptor").single() as ServerDescriptor

    // A real descriptor from CollecTor of a relay that is a member of two families
    val realDescriptor = FamilyCertUtilTest::class.java
        .getResource("/descriptors/server-descriptor-two-families.txt")!!.readText()

    "the family keys of a real descriptor are verified" {
        val descriptor = parseServerDescriptor(realDescriptor)

        descriptor.familyCerts.size shouldBe 2
        descriptor.verifiedFamilyKeys() shouldBe listOf(
            "ed25519:9ZiuZBKvxIauADkUfqvvU17jtXWhrA8RVKrkZgMEvZ4",
            "ed25519:vO6m/idEC83oHLQcFzeZDoMfYNUsT0Q+0xbhCUuMrFg",
        )
    }

    "a real descriptor whose family certificate does not match its identity key has no family keys" {
        val otherMasterKey = Base64.getEncoder().withoutPadding().encodeToString(relayKey)
        val descriptor = parseServerDescriptor(
            realDescriptor.replace(Regex("master-key-ed25519 .*"), "master-key-ed25519 $otherMasterKey")
                // The identity-ed25519 cert must match the master key, metrics-lib checks that
                .replace(Regex("identity-ed25519\n-----BEGIN ED25519 CERT-----\n[^-]*-----END ED25519 CERT-----\n"), "")
        )

        descriptor.familyCerts.size shouldBe 2
        descriptor.verifiedFamilyKeys().shouldBeEmpty()
    }
})

private fun KeyPair.rawPublicKey(): ByteArray = public.encoded.takeLast(32).toByteArray()
