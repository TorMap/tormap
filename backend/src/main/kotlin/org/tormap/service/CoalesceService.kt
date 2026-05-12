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

    fun submit(key: String, task: () -> Unit): CompletableFuture<Void> {
        val state = states.computeIfAbsent(key) { CoalesceState() }

        synchronized(state.monitor) {
            if (state.running) {
                state.pending = true
                return CompletableFuture.completedFuture(null)
            }
            state.running = true
        }

        return CompletableFuture.runAsync({
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

                    if (!continueOrFinish(state)) {
                        finishedAllReruns = true
                        break
                    }
                }
            } finally {
                if (!finishedAllReruns) {
                    synchronized(state.monitor) {
                        state.running = false
                    }
                }
            }
        }, coalesceExecutor)
    }

    private fun continueOrFinish(state: CoalesceState): Boolean {
        synchronized(state.monitor) {
            if (state.pending) {
                state.pending = false
                return true
            }
            state.running = false
            return false
        }
    }
}
