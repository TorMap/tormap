package org.tormap.util

import com.sun.management.HotSpotDiagnosticMXBean
import java.lang.management.ManagementFactory

/**
 * JVM options the backend expects to be set (see `hardenedJvmFlags` in build.gradle.kts).
 * The Docker image sets them; anyone starting the JAR another way has to pass them too.
 */
object JvmHardening {
    const val BASE64_INTRINSICS_OPTION = "UseBASE64Intrinsics"
    const val BASE64_INTRINSICS_FLAG = "-XX:-$BASE64_INTRINSICS_OPTION"

    /**
     * Whether the JVM uses its CPU-specific Base64 intrinsic. False when the option is off or does not exist on this
     * JVM or platform.
     */
    fun base64IntrinsicsEnabled(
        readOption: (String) -> String? = ::readVmOption,
    ): Boolean = readOption(BASE64_INTRINSICS_OPTION).toBoolean()

    private fun readVmOption(name: String): String? = try {
        ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean::class.java)?.getVMOption(name)?.value
    } catch (_: IllegalArgumentException) {
        null
    }
}
