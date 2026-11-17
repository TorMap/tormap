package org.tormap.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class JvmHardeningTest : StringSpec({
    "tests run with the Base64 intrinsic disabled, like the Docker image" {
        JvmHardening.base64IntrinsicsEnabled() shouldBe false
    }

    "reports the option's value" {
        JvmHardening.base64IntrinsicsEnabled { "true" } shouldBe true
        JvmHardening.base64IntrinsicsEnabled { "false" } shouldBe false
    }

    "treats a missing option as disabled" {
        JvmHardening.base64IntrinsicsEnabled { null } shouldBe false
    }
})
