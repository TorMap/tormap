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
    private data class CoalesceState(
        val running: AtomicBoolean = AtomicBoolean(false),
        val pending: AtomicBoolean = AtomicBoolean(false),
    )

    private val states = ConcurrentHashMap<String, CoalesceState>()

    fun submitAsync(key: String, task: () -> Unit): CompletableFuture<Void> {
        val state = states.computeIfAbsent(key) { CoalesceState() }

        if (!state.running.compareAndSet(false, true)) {
            state.pending.set(true)
            return CompletableFuture.completedFuture(null)
        }

        return CompletableFuture.runAsync({
            try {
                while (true) {
                    state.pending.set(false)

                    try {
                        task()
                    } catch (ex: Exception) {
                        logger.error("Coalesced task failed for key={}", key, ex)
                        break
                    }

                    if (state.pending.compareAndSet(true, false)) {
                        continue
                    }

                    state.running.set(false)

                    if (state.pending.compareAndSet(true, false) && state.running.compareAndSet(false, true)) {
                        continue
                    }

                    break
                }
            } finally {
                state.running.set(false)
            }
        }, coalesceExecutor)
    }
}
