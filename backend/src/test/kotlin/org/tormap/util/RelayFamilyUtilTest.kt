package org.tormap.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.tormap.mockRelayDetails

@SpringBootTest
@ActiveProfiles("test")
class RelayFamilyUtilTest : StringSpec({
    val relay1 = mockRelayDetails('A')
    val relay2 = mockRelayDetails('B')
    val relay3 = mockRelayDetails('C')

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
        "${relay3.nickname}, \$${relay2.fingerprint}",
        relay2.nickname,
        "${relay3.nickname}, ${relay2.nickname}",
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

