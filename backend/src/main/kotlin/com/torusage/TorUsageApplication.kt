package com.torusage

import com.torusage.common.logger
import com.torusage.database.repository.RelaySummaryRepository
import com.torusage.database.entity.RelaySummary
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
    val relaySummaryRepository: RelaySummaryRepository,
    val entityManager: EntityManager
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        logger().info("TorUsage backend started successfully, running in timezone: " + ZonedDateTime.now().zone)
        val testList = listOf("a","b")
        val test = RelaySummary("test","test", a = testList, true)
        relaySummaryRepository.save(test)
        logger().info(relaySummaryRepository.findByN("test")!!.f)
        var a = relaySummaryRepository.findByN("test")!!.a
        var testrepo = relaySummaryRepository.findAll()
    }

}

fun main(args: Array<String>) {
    runApplication<TorUsageApplication>(*args)
}
