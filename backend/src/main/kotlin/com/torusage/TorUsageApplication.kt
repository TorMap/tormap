package com.torusage

import com.torusage.common.logger
import com.torusage.database.RelaySummaryRepositories
import com.torusage.database.entities.RelaySummary
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.ZonedDateTime
import javax.persistence.EntityManager


@SpringBootApplication
@EnableScheduling
class TorUsageApplication(
    val relaySummaryRepositories: RelaySummaryRepositories,
    val entityManager: EntityManager
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        logger().info("TorUsage backend started successfully, running in timezone: " + ZonedDateTime.now().zone)
        val testList = listOf("a","b")
        val test = RelaySummary("test","test", a = testList, true)
        relaySummaryRepositories.save(test)
        logger().info(relaySummaryRepositories.findByN("test")!!.f)
        var a = relaySummaryRepositories.findByN("test")!!.a
        var testrepo = relaySummaryRepositories.findAll()
    }

}

fun main(args: Array<String>) {
    runApplication<TorUsageApplication>(*args)
}
