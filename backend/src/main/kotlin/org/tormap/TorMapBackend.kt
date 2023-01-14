package org.tormap

import mu.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.time.ZoneId

@SpringBootApplication
class TorMapBackend : ApplicationRunner {
    private val logger = KotlinLogging.logger { }
    override fun run(args: ApplicationArguments) {
        logger.info("TorMap backend started successfully, running in timezone: ${ZoneId.systemDefault()}")
    }
}

/**
 * Main method that starts backend application
 */
fun main(args: Array<String>) {
    runApplication<TorMapBackend>(*args)
}
