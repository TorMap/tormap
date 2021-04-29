package com.torusage

import com.torusage.common.logger
import com.torusage.database.DatabaseController
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.ZonedDateTime


@SpringBootApplication
class TorUsageApplication(
    val dbController: DatabaseController
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        logger().info("TorUsage backend started successfully, running in timezone: " + ZonedDateTime.now().zone)
        dbController.run()
    }

}

fun main(args: Array<String>) {
    runApplication<TorUsageApplication>(*args)
}
