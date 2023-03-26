package org.tormap.config

import com.maxmind.db.Metadata
import com.maxmind.geoip2.model.AnonymousIpResponse
import com.maxmind.geoip2.model.AsnResponse
import com.maxmind.geoip2.model.CityResponse
import com.maxmind.geoip2.model.ConnectionTypeResponse
import com.maxmind.geoip2.model.CountryResponse
import com.maxmind.geoip2.model.DomainResponse
import com.maxmind.geoip2.model.EnterpriseResponse
import com.maxmind.geoip2.model.IpRiskResponse
import com.maxmind.geoip2.model.IspResponse
import com.maxmind.geoip2.record.City
import com.maxmind.geoip2.record.Continent
import com.maxmind.geoip2.record.Country
import com.maxmind.geoip2.record.Location
import com.maxmind.geoip2.record.MaxMind
import com.maxmind.geoip2.record.Postal
import com.maxmind.geoip2.record.RepresentedCountry
import com.maxmind.geoip2.record.Subdivision
import com.maxmind.geoip2.record.Traits
import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.aot.hint.registerType
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
        // temporary hint, should be included into the official spring boot project
        hints.reflection().registerType<AutonomousSystemLookupConfig>(*MemberCategory.values())
        hints.reflection().registerType<DescriptorConfig>(*MemberCategory.values())
        hints.reflection().registerType<IpLookupConfig>(*MemberCategory.values())
        hints.reflection().registerType<LocationLookupConfig>(*MemberCategory.values())
        hints.reflection().registerType<MonthFamilyMembersCount>(*MemberCategory.values())
        hints.reflection().registerType<RateConfig>(*MemberCategory.values())
        hints.reflection().registerType<ScheduleConfig>(*MemberCategory.values())

        // maxmind db reflections
        hints.reflection().registerType<AnonymousIpResponse>(*MemberCategory.values())
        hints.reflection().registerType<AsnResponse>(*MemberCategory.values())
        hints.reflection().registerType<City>(*MemberCategory.values())
        hints.reflection().registerType<CityResponse>(*MemberCategory.values())
        hints.reflection().registerType<ConnectionTypeResponse>(*MemberCategory.values())
        hints.reflection().registerType<Continent>(*MemberCategory.values())
        hints.reflection().registerType<Country>(*MemberCategory.values())
        hints.reflection().registerType<CountryResponse>(*MemberCategory.values())
        hints.reflection().registerType<DomainResponse>(*MemberCategory.values())
        hints.reflection().registerType<EnterpriseResponse>(*MemberCategory.values())
        hints.reflection().registerType<IpRiskResponse>(*MemberCategory.values())
        hints.reflection().registerType<IspResponse>(*MemberCategory.values())
        hints.reflection().registerType<Location>(*MemberCategory.values())
        hints.reflection().registerType<MaxMind>(*MemberCategory.values())
        hints.reflection().registerType<Metadata>(*MemberCategory.values())
        hints.reflection().registerType<Postal>(*MemberCategory.values())
        hints.reflection().registerType<RepresentedCountry>(*MemberCategory.values())
        hints.reflection().registerType<Subdivision>(*MemberCategory.values())
        hints.reflection().registerType<Traits>(*MemberCategory.values())
    }
}
