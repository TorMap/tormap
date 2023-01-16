package org.tormap.database.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.tormap.relayDetailsMock

@SpringBootTest
@ActiveProfiles("test")
class RelayDetailsRepositoryTest(
    private val relayDetailsRepository: RelayDetailsRepositoryImpl,
) : StringSpec({
    val commonFamilyId = 0L
    val relay1 = relayDetailsMock('A')
    relay1.familyId = commonFamilyId

    val relay2 = relayDetailsMock('B')
    relay2.familyId = commonFamilyId

    val relay3 = relayDetailsMock('C')
    relay3.familyId = commonFamilyId + 1

    relayDetailsRepository.save(relay1)
    relayDetailsRepository.save(relay2)
    relayDetailsRepository.save(relay3)


    "find family identifiers by family id" {
        val familyIdentifiers = withContext(Dispatchers.IO) {
            relayDetailsRepository.findFamilyIdentifiers(listOf(commonFamilyId))
        }.first()
        familyIdentifiers.id shouldBe commonFamilyId
        familyIdentifiers.memberCount shouldBe 2
        familyIdentifiers.nicknames shouldBe "${relay1.nickname}, ${relay2.nickname}"
        familyIdentifiers.autonomousSystems shouldBe "${relay1.autonomousSystemName}, ${relay2.autonomousSystemName}"
    }
})

