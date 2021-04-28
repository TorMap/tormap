package com.torusage

import com.torusage.adapter.OnionooApiClient
import com.torusage.common.logger
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.time.ZonedDateTime


@SpringBootApplication
class TorUsageApplication(
    private val onionooApiClient: OnionooApiClient
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        logger().info("TorUsage backend started successfully, running in timezone: " + ZonedDateTime.now().zone)
        logger().info("Get test data from Onionoo API:")
        val response = onionooApiClient.getTorNodeDetails(limit = 15)
        logger().info("Relays count: ${response.relays.size}")
        logger().info("Bridges count: ${response.bridges.size}")
    }
}

fun main(args: Array<String>) {
    runApplication<TorUsageApplication>(*args)
}
