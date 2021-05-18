package com.torusage

import com.torusage.common.logger
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.ZonedDateTime


@SpringBootApplication
@EnableScheduling
class TorUsageApplication() : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        logger().info("TorUsage backend started successfully, running in timezone: " + ZonedDateTime.now().zone)
    }

}

fun main(args: Array<String>) {
    runApplication<TorUsageApplication>(*args)
}
