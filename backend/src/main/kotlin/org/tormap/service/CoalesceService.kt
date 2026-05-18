package org.tormap.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.tormap.util.logger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

@Service
class CoalesceService(
    @Qualifier("coalesceExecutor")
    private val coalesceExecutor: Executor,
) {
    private val logger = logger()

    /**
     * Latest-wins coalescing per key:
     * - At most one execution runs at a time per key.
     * - If submit happens while running, at most one rerun is queued.
     * - All submissions while running share the same future for that rerun.
     */
    private class Slot {
        val running = AtomicBoolean(false)
        val rerunRequested = AtomicBoolean(false)

        val monitor = Any()
        var nextFuture: CompletableFuture<Void>? = null
    }

    private val slots = ConcurrentHashMap<String, Slot>()

    fun submitAsync(key: String, task: () -> Unit): CompletableFuture<Void> {
        val slot = slots.computeIfAbsent(key) { Slot() }

        // If no run is in flight, start the loop and return a future for the first run.
        if (slot.running.compareAndSet(false, true)) {
            val firstFuture = CompletableFuture<Void>()
            runLoopAsync(key, slot, task, firstFuture)
            return firstFuture
        }

        // Already running: request one rerun (collapsed) and return the shared future for that rerun.
        slot.rerunRequested.set(true)
        synchronized(slot.monitor) {
            val existing = slot.nextFuture
            if (existing != null) return existing
            return CompletableFuture<Void>().also { slot.nextFuture = it }
        }
    }

    internal fun hasStateForKey(key: String): Boolean = slots.containsKey(key)

    private fun runLoopAsync(
        key: String,
        slot: Slot,
        task: () -> Unit,
        initialFuture: CompletableFuture<Void>,
    ) {
        CompletableFuture.runAsync(
            {
                var currentFuture: CompletableFuture<Void>? = initialFuture
                var keepGoing = true

                while (keepGoing) {
                    try {
                        task()
                        currentFuture?.complete(null)
                    } catch (ex: Exception) {
                        logger.error("Coalesced task failed for key={}", key, ex)
                        currentFuture?.completeExceptionally(ex)
                        // Keep existing behavior: stop reruns for this cycle after a failed execution.
                        keepGoing = false
                        continue
                    }

                    // If no rerun requested, finish.
                    if (!slot.rerunRequested.getAndSet(false)) {
                        keepGoing = false
                        continue
                    }

                    // Promote the shared next future for the rerun. If none exists (rare race), create one.
                    currentFuture = synchronized(slot.monitor) {
                        val next = slot.nextFuture ?: CompletableFuture<Void>()
                        slot.nextFuture = null
                        next
                    }
                }

                // Mark not running and remove slot. If a submit races here, it will start a new loop.
                slot.running.set(false)
                slots.remove(key, slot)
            },
            coalesceExecutor,
        )
    }
}
