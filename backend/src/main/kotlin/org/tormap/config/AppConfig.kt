package org.tormap.config

import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportRuntimeHints
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.filter.ShallowEtagHeaderFilter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.tormap.database.repository.MonthFamilyMembersCount

/**
 * Add all application configuration here and not in @SpringBootApplication class
 */
@Configuration
@EnableScheduling
@ImportRuntimeHints(IpLookupResourceRegistrar::class)
@EnableConfigurationProperties(ScheduleConfig::class, DescriptorConfig::class, IpLookupConfig::class)
class AppConfig : WebMvcConfigurer {

    /**
     * Allow any origin for any route mapping
     */
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**")
            .allowedMethods("*")
            .allowedHeaders("*")
            .allowedOrigins("*")
    }

    /**
     * Include an unique ETag hash for each response to enable client side caching.
     */
    @Bean
    fun shallowEtagHeaderFilter() = ShallowEtagHeaderFilter().apply { isWriteWeakETag = true }
}

// see https://github.com/spring-projects/spring-boot/issues/33400
private class IpLookupResourceRegistrar : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        // Temporary hint, should be included into the official spring boot project
        hints.reflection().registerType(MonthFamilyMembersCount::class.java, *MemberCategory.values())
        hints.reflection().registerType(ScheduleConfig::class.java, *MemberCategory.values())
        hints.reflection().registerType(RateConfig::class.java, *MemberCategory.values())
        hints.reflection().registerType(DescriptorConfig::class.java, *MemberCategory.values())
        hints.reflection().registerType(IpLookupConfig::class.java, *MemberCategory.values())
        hints.reflection().registerType(LocationLookupConfig::class.java, *MemberCategory.values())
        hints.reflection().registerType(AutonomousSystemLookupConfig::class.java, *MemberCategory.values())
    }
}
