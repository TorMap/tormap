package org.tormap

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.tormap.util.JvmHardening
import org.tormap.util.logger
import java.time.ZonedDateTime

@SpringBootApplication
class TorMapBackend : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val logger = logger()
        logger.info("TorMap backend started successfully, running in timezone: {}", ZonedDateTime.now().zone)
        if (JvmHardening.base64IntrinsicsEnabled()) {
            logger.warn("Start the JVM with {} (the Docker image sets it). See backend/README.md.", JvmHardening.BASE64_INTRINSICS_FLAG)
        }
    }

}

/**
 * Main method that starts backend application
 */
fun main(args: Array<String>) {
    runApplication<TorMapBackend>(*args)
}
