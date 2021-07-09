package com.torusage

import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Get logger for certain class
 */
@Suppress("unused")
inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)

suspend fun <A, B> Iterable<A>.mapParallel(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}

fun <T> Iterable<T>.forEachParallel(action: suspend (T) -> Unit) = runBlocking {
    for (item in this@forEachParallel) launch(Dispatchers.Default) {
        action(item)
    }
}

fun String.commaSeparatedToList() = this.split(",").map { it.trim() }