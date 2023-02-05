package org.tormap

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.tormap.config.IpLookupConfig
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
        ipLookupConfig.locationLookup.dbipDatabaseFile.isReadable shouldBe true
        ipLookupConfig.autonomousSystemLookup.maxmindDatabaseFile.isReadable shouldBe true
    }
})

