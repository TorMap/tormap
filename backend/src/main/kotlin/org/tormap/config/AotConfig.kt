package org.tormap.config

import org.hibernate.dialect.PostgreSQLPGObjectJdbcType
import org.springframework.aot.hint.MemberCategory.*
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportRuntimeHints
import org.tormap.config.value.DescriptorConfig
import org.tormap.config.value.IpLookupConfig
import org.tormap.config.value.ScheduleConfig

@Configuration
@ImportRuntimeHints(PSQLDriverAotRuntimeReflectionHintRegistrar::class)
@RegisterReflectionForBinding(ScheduleConfig::class, DescriptorConfig::class, IpLookupConfig::class)
class AotConfig

// see https://github.com/spring-projects/spring-boot/issues/33400
internal class PSQLDriverAotRuntimeReflectionHintRegistrar : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        // Temporary hint, should be included into the official spring boot project
        hints.reflection().registerTypeIfPresent(classLoader, "org.postgresql.util.PGobject") { hint ->
            hint.withMembers(INVOKE_PUBLIC_CONSTRUCTORS, INTROSPECT_PUBLIC_METHODS)
                .onReachableType(PostgreSQLPGObjectJdbcType::class.java)
        }
        hints.resources().registerPattern("ip-lookup/location/dbip/dbip-city-lite.mmdb")
        hints.resources().registerPattern("ip-lookup/autonomous-system/maxmind/GeoLite2-ASN.mmdb")
    }
}
