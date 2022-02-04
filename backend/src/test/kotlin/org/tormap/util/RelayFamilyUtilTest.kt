package org.tormap.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.tormap.relayDetailsMock
import java.lang.Exception

@SpringBootTest
@ActiveProfiles("test")
class RelayFamilyUtilTest : StringSpec({
    val relay1 = relayDetailsMock()
    relay1.fingerprint = "A".repeat(40)
    relay1.nickname = "A"

    val relay2 = relayDetailsMock()
    relay2.fingerprint = "B".repeat(40)
    relay2.nickname = "B"

    val relay3 = relayDetailsMock()
    relay3.fingerprint = "C".repeat(40)
    relay3.nickname = "C"

    listOf(
        relay2.nickname,
        "$" + relay2.fingerprint,
    ).forEach {
        "get relay family member (familyEntry=$it)" {
            relay1.familyEntries = "\$${relay2.fingerprint}, ${relay2.nickname}"
            relay2.familyEntries = "\$${relay1.fingerprint}, ${relay1.nickname}"
            relay1.getFamilyMember(it, listOf(relay1, relay2))
        }
    }

    listOf(
        relay2.nickname,
        "\$${relay2.fingerprint}",
    ).forEach {
        "get relay family member returns null (familyEntry=$it)" {
            relay1.familyEntries = "\$${relay3.fingerprint}, ${relay3.nickname}"
            relay2.familyEntries = "\$${relay3.fingerprint}, ${relay3.nickname}"
            relay1.getFamilyMember(it, listOf(relay1, relay2))
        }
    }

    listOf(
        " ",
        "A".repeat(20),
        "$" + "A".repeat(41),
        "???",
    ).forEach {
        "get relay family member throws on wrong familyEntry format ($it)" {
            shouldThrow<Exception> {
                relay1.getFamilyMember(it, listOf(relay1, relay2))
            }
        }
    }

    listOf(
        "\$${relay2.fingerprint}",
        "C, \$${relay2.fingerprint}",
        relay2.nickname,
        "C, ${relay2.nickname}",
    ).forEach {
        "relay confirms family member (familyEntries=$it)" {
            relay1.familyEntries = it
            relay1.confirmsFamilyMember(relay2) shouldBe true
        }
    }


    "relay does not confirm family member" {
        relay1.familyEntries = "\$${relay3.fingerprint}, ${relay3.nickname}"
        relay2.familyEntries = "\$${relay1.fingerprint}, ${relay1.nickname}"

        relay1.confirmsFamilyMember(relay2) shouldBe false
    }
})

