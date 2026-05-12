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
        var currentFuture: CompletableFuture<Void>? = null,
        var pendingFuture: CompletableFuture<Void>? = null,
    )

    private val states = ConcurrentHashMap<String, CoalesceState>()

    fun submitAsync(key: String, task: () -> Unit): CompletableFuture<Void> {
        while (true) {
            val state = states.computeIfAbsent(key) { CoalesceState() }
            var shouldStartTask = false
            var completionFuture: CompletableFuture<Void>? = null

            synchronized(state.monitor) {
                // Another thread may have completed and removed this state, then a new one could have been
                // inserted for the same key. If this state is stale, retry with the current mapped state.
                if (states[key] !== state) {
                    return@synchronized
                }
                if (state.running) {
                    state.pending = true
                    if (state.pendingFuture == null) {
                        state.pendingFuture = CompletableFuture()
                    }
                    return state.pendingFuture!!
                }
                state.running = true
                if (state.currentFuture == null) {
                    state.currentFuture = CompletableFuture()
                }
                completionFuture = state.currentFuture
                shouldStartTask = true
            }

            if (shouldStartTask) {
                runTaskAsync(key, state, task)
                return completionFuture!!
            }
        }
    }

    internal fun hasStateForKey(key: String): Boolean = states.containsKey(key)

    private fun runTaskAsync(key: String, state: CoalesceState, task: () -> Unit) {
        CompletableFuture.runAsync(
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
                        finishCurrentState(key, state)
                    }
                }
            },
            coalesceExecutor
        )
    }

    private fun continueOrFinish(key: String, state: CoalesceState): Boolean {
        var completedFuture: CompletableFuture<Void>? = null
        var continueWithRerun = false
        synchronized(state.monitor) {
            if (state.pending) {
                state.pending = false
                completedFuture = state.currentFuture
                state.currentFuture = state.pendingFuture
                state.pendingFuture = null
                continueWithRerun = true
            } else {
                state.running = false
                completedFuture = state.currentFuture
                state.currentFuture = null
                state.pendingFuture = null
                states.remove(key, state)
            }
        }
        completedFuture?.complete(null)
        return continueWithRerun
    }

    private fun finishCurrentState(key: String, state: CoalesceState) {
        var currentFuture: CompletableFuture<Void>? = null
        var pendingFuture: CompletableFuture<Void>? = null
        synchronized(state.monitor) {
            if (!state.running) {
                return
            }
            state.running = false
            state.pending = false
            currentFuture = state.currentFuture
            pendingFuture = state.pendingFuture
            state.currentFuture = null
            state.pendingFuture = null
            states.remove(key, state)
        }
        currentFuture?.complete(null)
        pendingFuture?.complete(null)
    }
}
