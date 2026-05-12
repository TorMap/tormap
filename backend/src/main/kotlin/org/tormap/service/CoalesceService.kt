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
    private data class CoalesceState(
        val monitor: Any = Any(),
        var running: Boolean = false,
        var pending: Boolean = false,
    )

    private val states = ConcurrentHashMap<String, CoalesceState>()

    fun submitAsync(key: String, task: () -> Unit): CompletableFuture<Void> {
        while (true) {
            val state = states.computeIfAbsent(key) { CoalesceState() }
            var shouldStartTask = false

            synchronized(state.monitor) {
                // Another thread may have completed and removed this state, then a new one could have been
                // inserted for the same key. If this state is stale, retry with the current mapped state.
                if (states[key] !== state) {
                    return@synchronized
                }
                if (state.running) {
                    state.pending = true
                    return CompletableFuture.completedFuture(null)
                }
                state.running = true
                shouldStartTask = true
            }

            if (shouldStartTask) {
                return runTaskAsync(key, state, task)
            }
        }
    }

    internal fun hasStateForKey(key: String): Boolean = states.containsKey(key)

    private fun runTaskAsync(key: String, state: CoalesceState, task: () -> Unit): CompletableFuture<Void> {
        return CompletableFuture.runAsync(
            {
            var finishedAllReruns = false
            try {
                while (true) {
                    try {
                        task()
                    } catch (ex: Exception) {
                        // Keep existing behavior: stop reruns for this cycle after a failed execution.
                        logger.error("Coalesced task failed for key={}", key, ex)
                        break
                    }

                    if (!continueOrFinish(key, state)) {
                        finishedAllReruns = true
                        break
                    }
                }
            } finally {
                if (!finishedAllReruns) {
                    synchronized(state.monitor) {
                        state.running = false
                        state.pending = false
                        states.remove(key, state)
                    }
                }
            }
        },
            coalesceExecutor
        )
    }

    private fun continueOrFinish(key: String, state: CoalesceState): Boolean {
        synchronized(state.monitor) {
            if (state.pending) {
                state.pending = false
                return true
            }
            state.running = false
            states.remove(key, state)
            return false
        }
    }
}
