package org.tormap.database.repository

import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class RelayLocationRepositoryTest(
    private val relayLocationRepository: RelayLocationRepositoryImpl,
) : StringSpec({

    beforeEach {
        withContext(Dispatchers.IO) {
            relayLocationRepository.deleteAll()
        }
    }

    "findDistinctDays" {
        // TODO
    }

    "findAllUsingDay" {
        // TODO
    }
})

