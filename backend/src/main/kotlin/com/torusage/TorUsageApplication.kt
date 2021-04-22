package com.torusage

import com.torusage.common.logger
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
class TorUsageApplication: ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        logger().info("TorUsage backend started successfully, running in timezone: " + ZonedDateTime.now().zone)
        logger().info("Get test data from Onionoo Service:")
        val client = HttpClient.newBuilder().build();
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://onionoo.torproject.org/summary?limit=5"))
            .build();
        val response = client.send(request, HttpResponse.BodyHandlers.ofString());
        logger().info(response.body())
    }

}

fun main(args: Array<String>) {
    runApplication<TorUsageApplication>(*args)
}
