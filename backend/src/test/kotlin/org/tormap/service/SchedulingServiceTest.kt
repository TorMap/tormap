package org.tormap.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.Future

class SchedulingServiceTest : StringSpec({
    // SchedulingService is annotated with @Async, which rejects other return types at runtime
    "all scheduled methods return void or Future" {
        val scheduledMethods = SchedulingService::class.java.declaredMethods
            .filter { it.isAnnotationPresent(Scheduled::class.java) }
        scheduledMethods.shouldNotBeEmpty()

        scheduledMethods
            .filterNot { it.returnType == Void.TYPE || Future::class.java.isAssignableFrom(it.returnType) }
            .map { "${it.name}: ${it.returnType}" }
            .shouldBeEmpty()
    }
})
