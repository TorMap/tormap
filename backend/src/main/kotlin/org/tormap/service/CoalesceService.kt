package org.tormap.service

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.tormap.util.logger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

@Service
class CoalesceService {
    private val logger = logger()
    private val runningLocks = ConcurrentHashMap<String, ReentrantLock>()
    private val pendingFlags = ConcurrentHashMap<String, AtomicBoolean>()

    @Async("coalesceExecutor")
    fun submitAsync(key: String, task: () -> Unit): CompletableFuture<Void> {
        val pending = pendingFlags.computeIfAbsent(key) { AtomicBoolean(false) }
        val lock = runningLocks.computeIfAbsent(key) { ReentrantLock() }

        // If a task is already running for this key, just mark "pending" and return
        if (!lock.tryLock()) {
            pending.set(true)
            return CompletableFuture.completedFuture(null)
        }

        try {
            do {
                // Clear before running; if anything comes in while running,
                // it will set the flag back to true.
                pending.set(false)

                try {
                    task()
                } catch (ex: Exception) {
                    logger.error("Coalesced task failed for key={}", key, ex)
                    // Avoid tight error loops; break instead of immediately rerunning.
                    break
                }

                // Loop once more if someone called submit(key, ...) while we were running.
            } while (pending.compareAndSet(true, false))
        } finally {
            lock.unlock()

            // Best-effort cleanup of maps for ephemeral keys
            if (!lock.isLocked) {
                runningLocks.remove(key, lock)
                pendingFlags.remove(key, pending)
            }
        }

        return CompletableFuture.completedFuture(null)
    }
}
