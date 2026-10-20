package org.tormap.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.transaction.support.TransactionTemplate
import org.tormap.database.repository.MonthFamilyMembersCount
import org.tormap.database.repository.RelayDetailsRepositoryImpl
import java.util.concurrent.CompletableFuture
import javax.sql.DataSource

class RelayDetailsUpdateServiceTest : StringSpec({
    "computeAllMissingFamiliesAndEvictCache evicts the cache only after the families are computed" {
        val repository = mockk<RelayDetailsRepositoryImpl>(relaxed = true)
        val cacheService = mockk<CacheService>(relaxed = true)
        val coalesceService = mockk<CoalesceService>()
        val familiesOfMonth = mapOf("2024-01" to CompletableFuture<Void>(), "2024-02" to CompletableFuture<Void>())
        every { repository.findDistinctMonthFamilyMemberCount() } returns listOf(
            MonthFamilyMembersCount("2024-01", 0),
            MonthFamilyMembersCount("2024-02", 0),
            // Months that already have families are skipped
            MonthFamilyMembersCount("2024-03", 5),
        )
        every { coalesceService.submitAsync(any(), any()) } answers {
            familiesOfMonth.getValue(firstArg<String>().removePrefix("computeFamilies-"))
        }
        val service = RelayDetailsUpdateService(
            repository,
            mockk(relaxed = true),
            cacheService,
            coalesceService,
            mockk<TransactionTemplate>(relaxed = true),
            mockk<DataSource>(relaxed = true),
        )

        service.computeAllMissingFamiliesAndEvictCache()

        verify(exactly = 1) { coalesceService.submitAsync("computeFamilies-2024-01", any()) }
        verify(exactly = 1) { coalesceService.submitAsync("computeFamilies-2024-02", any()) }
        verify(exactly = 0) { coalesceService.submitAsync("computeFamilies-2024-03", any()) }
        familiesOfMonth.getValue("2024-01").complete(null)
        verify(exactly = 0) { cacheService.evictRelayLocationsPerDay(any()) }

        // A failed month does not prevent the eviction
        familiesOfMonth.getValue("2024-02").completeExceptionally(IllegalStateException("failed"))
        verify(exactly = 1) { cacheService.evictRelayLocationsPerDay(setOf("2024-01", "2024-02")) }
    }

    "computeFamilies completes once all months are done" {
        val coalesceService = mockk<CoalesceService>()
        val future = CompletableFuture<Void>()
        every { coalesceService.submitAsync(any(), any()) } returns future
        val service = RelayDetailsUpdateService(
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            coalesceService,
            mockk<TransactionTemplate>(relaxed = true),
            mockk<DataSource>(relaxed = true),
        )

        val result = service.computeFamilies(setOf("2024-01"))
        future.complete(null)
        result.isDone.shouldBeTrue()
        service.computeFamilies(emptySet()).isDone.shouldBeTrue()
    }
})
