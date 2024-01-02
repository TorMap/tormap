package org.tormap

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.tormap.config.value.IpLookupConfig
import java.io.File

@SpringBootTest
@ActiveProfiles("test")
class TorMapBackendTest(
    @Value("\${spring.security.user.passwordFile}")
    private val adminPasswordFile: String,

    private val ipLookupConfig: IpLookupConfig,
) : StringSpec({
    "context loads" {}

    "admin password file gets created" {
        File(adminPasswordFile).exists() shouldBe true
    }

    "ip-lookup files exist" {
        javaClass.getResource(ipLookupConfig.locationDatabaseFile) shouldNotBe null
        javaClass.getResource(ipLookupConfig.autonomousSystemDatabaseFile) shouldNotBe null
    }
})

