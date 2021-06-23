package com.torusage

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.ZonedDateTime

/**
 * Configures basic Spring Boot application
 */
@SpringBootApplication
class TorUsageApplication : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        logger().info("Backend started successfully, running in timezone: " + ZonedDateTime.now().zone)
    }

}

/**
 * Main method to start backend application
 */
fun main(args: Array<String>) {
    runApplication<TorUsageApplication>(*args)
}
