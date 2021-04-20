package com.torusage

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TorUsageApplication

fun main(args: Array<String>) {
    runApplication<TorUsageApplication>(*args)
}
