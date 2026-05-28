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
        while (true) {
            val slot = slots.computeIfAbsent(key) { Slot() }
            var firstFuture: CompletableFuture<Void>? = null

            val queuedFuture = synchronized(slot.monitor) {
                if (slots[key] !== slot) return@synchronized null

                // If no run is in flight, start the loop and return a future for the first run.
                if (!slot.running.get()) {
                    firstFuture = CompletableFuture<Void>()
                    slot.running.set(true)
                    return@synchronized null
                }

                // Already running: request one rerun (collapsed) and return the shared future for that rerun.
                slot.rerunRequested.set(true)
                val existing = slot.nextFuture
                if (existing != null) return@synchronized existing
                CompletableFuture<Void>().also { slot.nextFuture = it }
            }

            if (queuedFuture != null) return queuedFuture
            val startFuture = firstFuture
            if (startFuture != null) {
                runLoopAsync(key, slot, task, startFuture)
                return startFuture
            }
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
                        synchronized(slot.monitor) {
                            val pendingNext = slot.nextFuture
                            slot.nextFuture = null
                            slot.rerunRequested.set(false)
                            slot.running.set(false)
                            slots.remove(key, slot)
                            pendingNext?.completeExceptionally(ex)
                        }
                        // Keep existing behavior: stop reruns for this cycle after a failed execution.
                        keepGoing = false
                    }

                    synchronized(slot.monitor) {
                        // If no rerun requested, finish.
                        if (!slot.rerunRequested.getAndSet(false)) {
                            slot.running.set(false)
                            slots.remove(key, slot)
                            keepGoing = false
                        } else {
                            // Promote the shared next future for the rerun. If none exists (rare race), create one.
                            val next = slot.nextFuture ?: CompletableFuture<Void>()
                            slot.nextFuture = null
                            currentFuture = next
                        }
                    }
                }
            },
            coalesceExecutor,
        )
    }
}
