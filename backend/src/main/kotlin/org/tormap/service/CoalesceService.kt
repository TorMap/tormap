package org.tormap.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.tormap.util.logger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

@Service
class CoalesceService(
    @Qualifier("coalesceExecutor")
    private val coalesceExecutor: Executor,
) {
    private val logger = logger()

    /**
     * Latest-wins coalescing per key:
     * - At most one execution runs at a time per key.
     * - If submit happens while running, at most one rerun is queued, using the latest submitted task.
     * - All submissions while running share the same future for that rerun.
     *
     * A slot is present in [slots] exactly while a run loop for its key is active.
     * All fields are guarded by the slot's monitor.
     */
    private class Slot {
        var pendingTask: (() -> Unit)? = null
        var pendingFuture: CompletableFuture<Void>? = null
    }

    private val slots = ConcurrentHashMap<String, Slot>()

    fun submitAsync(key: String, task: () -> Unit): CompletableFuture<Void> {
        while (true) {
            val newSlot = Slot()
            val slot = slots.putIfAbsent(key, newSlot)
            if (slot == null) {
                val future = CompletableFuture<Void>()
                startLoop(key, newSlot, task, future)
                return future
            }

            synchronized(slot) {
                // The loop removes its slot under this monitor, so a slot still mapped here is guaranteed to pick up the rerun.
                if (slots[key] === slot) {
                    slot.pendingTask = task
                    return slot.pendingFuture ?: CompletableFuture<Void>().also { slot.pendingFuture = it }
                }
            }
            // The loop finished between lookup and lock: retry and start a new one.
        }
    }

    internal fun hasStateForKey(key: String): Boolean = slots.containsKey(key)

    private fun startLoop(
        key: String,
        slot: Slot,
        task: () -> Unit,
        future: CompletableFuture<Void>,
    ) {
        try {
            coalesceExecutor.execute { runLoop(key, slot, task, future) }
        } catch (ex: Throwable) {
            logger.error("Could not schedule coalesced task for key={}", key, ex)
            val pendingFuture = synchronized(slot) {
                slots.remove(key, slot)
                slot.pendingFuture
            }
            future.completeExceptionally(ex)
            pendingFuture?.completeExceptionally(ex)
        }
    }

    private fun runLoop(
        key: String,
        slot: Slot,
        initialTask: () -> Unit,
        initialFuture: CompletableFuture<Void>,
    ) {
        var task = initialTask
        var future = initialFuture

        while (true) {
            val failure = try {
                task()
                null
            } catch (ex: Throwable) {
                logger.error("Coalesced task failed for key={}", key, ex)
                ex
            }

            // Release or hand over the slot before completing the future, so callers observe a consistent state.
            val next = synchronized(slot) {
                val nextTask = slot.pendingTask
                val nextFuture = slot.pendingFuture
                slot.pendingTask = null
                slot.pendingFuture = null
                if (nextTask == null || nextFuture == null) {
                    slots.remove(key, slot)
                    null
                } else {
                    nextTask to nextFuture
                }
            }

            if (failure == null) future.complete(null) else future.completeExceptionally(failure)

            if (next == null) return
            task = next.first
            future = next.second
        }
    }
}
