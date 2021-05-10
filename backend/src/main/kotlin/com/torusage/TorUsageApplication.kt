package com.torusage

import com.torusage.adapter.OnionooApiClient
import com.torusage.adapter.TorNodeType
import com.torusage.common.logger
import com.torusage.database.RelaySummaryRepositories
import com.torusage.model.RelaySummary
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.ZonedDateTime
import javax.persistence.EntityManager


@SpringBootApplication
class TorUsageApplication(
    val relaySummaryRepositories: RelaySummaryRepositories,
    val entityManager: EntityManager
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        logger().info("TorUsage backend started successfully, running in timezone: " + ZonedDateTime.now().zone)
        val test = RelaySummary("test","test", "test", true)
        relaySummaryRepositories.save(test)
    }

}

fun main(args: Array<String>) {
    runApplication<TorUsageApplication>(*args)
}
