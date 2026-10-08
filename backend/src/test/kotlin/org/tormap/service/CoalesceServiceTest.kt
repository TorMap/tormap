package org.tormap.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class CoalesceServiceTest : StringSpec({
    val executor = Executors.newCachedThreadPool()
    val coalesceService = CoalesceService(executor)

    afterSpec { executor.shutdownNow() }

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
        second.isDone shouldBe false
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
            try {
                maxActive.accumulateAndGet(current, ::maxOf)
                val run = runs.incrementAndGet()
                if (run == 1) {
                    started.countDown()
                    release.await(5, TimeUnit.SECONDS)
                }
            } finally {
                active.decrementAndGet()
            }
        }

        val first = coalesceService.submitAsync("coalesce-serial", task)

        started.await(5, TimeUnit.SECONDS) shouldBe true
        val queued = List(10) { coalesceService.submitAsync("coalesce-serial", task) }
        queued.forEach { it shouldBeSameInstanceAs queued.first() }
        release.countDown()

        first.get(5, TimeUnit.SECONDS)
        queued.first().get(5, TimeUnit.SECONDS)
        maxActive.get() shouldBe 1
        runs.get() shouldBe 2
    }

    "runs the latest submitted task for the queued rerun" {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executed = mutableListOf<String>()

        val first = coalesceService.submitAsync("coalesce-latest") {
            synchronized(executed) { executed += "first" }
            started.countDown()
            release.await(5, TimeUnit.SECONDS)
        }
        started.await(5, TimeUnit.SECONDS) shouldBe true
        coalesceService.submitAsync("coalesce-latest") { synchronized(executed) { executed += "stale" } }
        val latest = coalesceService.submitAsync("coalesce-latest") { synchronized(executed) { executed += "latest" } }
        release.countDown()

        first.get(5, TimeUnit.SECONDS)
        latest.get(5, TimeUnit.SECONDS)
        synchronized(executed) { executed.toList() } shouldBe listOf("first", "latest")
    }

    "still runs a queued rerun after the running task fails" {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val rerun = AtomicInteger(0)

        val first = coalesceService.submitAsync("coalesce-failure") {
            started.countDown()
            release.await(5, TimeUnit.SECONDS)
            throw IllegalStateException("boom")
        }
        started.await(5, TimeUnit.SECONDS) shouldBe true
        val second = coalesceService.submitAsync("coalesce-failure") { rerun.incrementAndGet() }
        release.countDown()

        val exception = shouldThrow<ExecutionException> { first.get(5, TimeUnit.SECONDS) }
        (exception.cause is IllegalStateException) shouldBe true
        second.get(5, TimeUnit.SECONDS)
        rerun.get() shouldBe 1
        coalesceService.hasStateForKey("coalesce-failure") shouldBe false
    }

    "releases the key when a task throws an Error" {
        val failed = coalesceService.submitAsync("coalesce-error") { throw AssertionError("fatal") }
        shouldThrow<ExecutionException> { failed.get(5, TimeUnit.SECONDS) }

        coalesceService.submitAsync("coalesce-error") {}.get(5, TimeUnit.SECONDS)
        coalesceService.hasStateForKey("coalesce-error") shouldBe false
    }

    "fails the future and releases the key when the executor rejects the task" {
        val rejecting = CoalesceService(Executor { throw RejectedExecutionException("shut down") })

        val future: CompletableFuture<Void> = rejecting.submitAsync("coalesce-rejected") {}

        shouldThrow<ExecutionException> { future.get(5, TimeUnit.SECONDS) }
        rejecting.hasStateForKey("coalesce-rejected") shouldBe false
    }

    "removes key state after task completion" {
        coalesceService.submitAsync("coalesce-cleanup") {}.get(5, TimeUnit.SECONDS)
        coalesceService.hasStateForKey("coalesce-cleanup") shouldBe false
    }
})
