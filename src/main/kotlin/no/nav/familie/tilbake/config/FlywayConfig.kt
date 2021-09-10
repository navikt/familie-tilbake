package no.nav.familie.tilbake.config

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean


@ConstructorBinding
data class FlywayConfig(private val role: String) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Bean
    fun flywayConfig(): FlywayConfigurationCustomizer {
        logger.info("DB-oppdateringer kjøres med rolle $role")
        return FlywayConfigurationCustomizer {
            it.initSql(String.format("SET ROLE \"%s\"", role))
        }
    }
}
