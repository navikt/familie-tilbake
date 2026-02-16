package no.nav.familie.tilbake.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment

data class FlywayConfig(
    private val role: String,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Bean
    fun flywayConfig(
        @Value("\${spring.flyway.placeholders.ignoreIfProd}") ignoreIfProd: String,
        environment: Environment,
    ): FlywayConfigurationCustomizer {
        logger.info("DB-oppdateringer kjøres med rolle $role")
        val isProd = environment.activeProfiles.contains("prod")
        val ignore = ignoreIfProd == "--"
        return FlywayConfigurationCustomizer {
            it.initSql(String.format("SET ROLE \"%s\"", role))
            if (isProd && !ignore) {
                throw RuntimeException("Prod profile-en har ikke riktig verdi for placeholder ignoreIfProd=$ignoreIfProd")
            }
            if (!isProd && ignore) {
                throw RuntimeException("Profile=${environment.activeProfiles} har ignoreIfProd=false")
            }
        }
    }
}
