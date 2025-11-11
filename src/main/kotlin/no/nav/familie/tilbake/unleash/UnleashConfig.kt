package no.nav.familie.tilbake.unleash

import io.getunleash.strategy.Strategy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(UnleashProperties::class)
open class UnleashConfig(
    private val featureToggleProperties: UnleashProperties,
    @Value("\${UNLEASH_SERVER_API_URL}") val apiUrl: String,
    @Value("\${UNLEASH_SERVER_API_TOKEN}") val apiToken: String,
    @Value("\${NAIS_APP_NAME}") val appName: String,
    private val strategies: List<Strategy>,
) {
    @Bean
    open fun unleashNext(): UnleashService =
        if (featureToggleProperties.enabled) {
            DefaultUnleashService(apiUrl = apiUrl, apiToken = apiToken, appName = appName, strategies = strategies)
        } else {
            logger.warn(
                "Funksjonsbryter-funksjonalitet er skrudd AV. " +
                    "isEnabled gir 'false' med mindre man har oppgitt en annen default verdi.",
            )
            lagDummyUnleashService()
        }

    private fun lagDummyUnleashService(): UnleashService =
        object : UnleashService {
            override fun isEnabled(
                toggleId: String,
                properties: Map<String, String>,
            ): Boolean = isEnabled(toggleId, false)

            override fun isEnabled(
                toggleId: String,
                defaultValue: Boolean,
            ): Boolean = System.getenv(toggleId).run { toBoolean() } || defaultValue

            override fun getFrontendToggles(): Map<String, Boolean> = emptyMap()

            override fun destroy() {
                // Dummy featureToggleService trenger ikke destroy, då den ikke har en unleash å lukke
            }
        }

    companion object {
        private val logger = LoggerFactory.getLogger(UnleashConfig::class.java)
    }
}

@ConfigurationProperties("unleash")
class UnleashProperties(
    val enabled: Boolean = true,
)

interface UnleashService : DisposableBean {
    fun isEnabled(
        toggleId: String,
        properties: Map<String, String>,
    ): Boolean

    fun isEnabled(
        toggleId: String,
        defaultValue: Boolean,
    ): Boolean

    fun getFrontendToggles(): Map<String, Boolean>
}
