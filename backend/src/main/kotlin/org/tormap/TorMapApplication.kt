package org.tormap

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.time.ZonedDateTime

/**
 * Configures basic Spring Boot application
 */
@SpringBootApplication
class TorMapApplication : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        logger().info("TorMap backend started successfully, running in timezone: " + ZonedDateTime.now().zone)
    }

}

/**
 * Main method that starts backend application
 */
fun main(args: Array<String>) {
    runApplication<TorMapApplication>(*args)
}
