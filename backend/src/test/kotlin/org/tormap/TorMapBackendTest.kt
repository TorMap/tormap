package org.tormap

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.tormap.config.value.IpLookupConfig

@SpringBootTest
@ActiveProfiles("test")
class TorMapBackendTest(
    private val ipLookupConfig: IpLookupConfig,
) : StringSpec({
    "context loads" {}

    "ip-lookup files exist" {
        javaClass.getResource(ipLookupConfig.locationDatabaseFile) shouldNotBe null
        javaClass.getResource(ipLookupConfig.autonomousSystemDatabaseFile) shouldNotBe null
    }
})
