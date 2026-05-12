package org.tormap.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@ActiveProfiles("test")
class CoalesceServiceTest(
    private val coalesceService: CoalesceService,
) : StringSpec({
    "reruns once when submit is called while a key is running" {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val runs = AtomicInteger(0)

        val task: () -> Unit = {
            val run = runs.incrementAndGet()
            if (run == 1) {
                started.countDown()
                release.await(5, TimeUnit.SECONDS)
            }
        }

        val first = coalesceService.submitAsync("coalesce-rerun", task)

        started.await(5, TimeUnit.SECONDS) shouldBe true
        val second = coalesceService.submitAsync("coalesce-rerun", task)
        release.countDown()

        first.get(5, TimeUnit.SECONDS)
        second.get(5, TimeUnit.SECONDS)
        runs.get() shouldBe 2
    }

    "never runs the same key concurrently even under repeated submissions" {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val active = AtomicInteger(0)
        val maxActive = AtomicInteger(0)
        val runs = AtomicInteger(0)

        val task: () -> Unit = {
            val current = active.incrementAndGet()
            maxActive.accumulateAndGet(current, ::maxOf)
            val run = runs.incrementAndGet()
            if (run == 1) {
                started.countDown()
                release.await(5, TimeUnit.SECONDS)
            }
            active.decrementAndGet()
        }

        val first = coalesceService.submitAsync("coalesce-serial", task)

        started.await(5, TimeUnit.SECONDS) shouldBe true
        repeat(10) { coalesceService.submitAsync("coalesce-serial", task) }
        release.countDown()

        first.get(5, TimeUnit.SECONDS)
        maxActive.get() shouldBe 1
        runs.get() shouldBe 2
    }

    "removes key state after task completion" {
        coalesceService.submitAsync("coalesce-cleanup") {}.get(5, TimeUnit.SECONDS)
        coalesceService.hasStateForKey("coalesce-cleanup") shouldBe false
    }
})
