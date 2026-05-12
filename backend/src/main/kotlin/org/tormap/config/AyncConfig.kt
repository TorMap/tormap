package org.tormap.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor

@Configuration
class AsyncConfig {

    @Bean("coalesceExecutor")
    fun coalesceExecutor(): Executor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 4
            maxPoolSize = 20
            queueCapacity = 200
            setThreadNamePrefix("coalesce-")
            setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
            initialize()
        }
}
