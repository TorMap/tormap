package org.tormap.controller

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.tormap.adapter.controller.SecurityTxtController
import java.time.Instant

class SecurityTxtControllerTest : StringSpec({

    "security.txt has the required fields and expires in less than a year" {
        SecurityTxtController.securityTxt(Instant.parse("2026-11-03T22:30:00Z")) shouldBe """
            Contact: https://github.com/TorMap/tormap/security/advisories/new
            Expires: 2027-05-02T00:00:00Z
            Policy: https://github.com/TorMap/tormap/security/policy
            Preferred-Languages: en

        """.trimIndent()
    }
})
