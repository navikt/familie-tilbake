package no.nav.familie.tilbake.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.UnleashContextProvider
import io.getunleash.strategy.Strategy
import io.getunleash.util.UnleashConfig

class DefaultUnleashService(
    val apiUrl: String,
    val apiToken: String,
    val appName: String,
    val strategies: List<Strategy>,
) : UnleashService {
    private val defaultUnleash: DefaultUnleash

    init {

        defaultUnleash =
            DefaultUnleash(
                UnleashConfig
                    .builder()
                    .appName(appName)
                    .unleashAPI("$apiUrl/api")
                    .apiKey(apiToken)
                    .unleashContextProvider(lagUnleashContextProvider())
                    .build(),
                *strategies.toTypedArray(),
            )
    }

    private fun lagUnleashContextProvider(): UnleashContextProvider =
        UnleashContextProvider {
            UnleashContext
                .builder()
                .appName(appName)
                .build()
        }

    override fun isEnabled(
        toggleId: String,
        defaultValue: Boolean,
    ): Boolean = defaultUnleash.isEnabled(toggleId, defaultValue)

    override fun isEnabled(
        toggleId: String,
        properties: Map<String, String>,
    ): Boolean {
        val builder = UnleashContext.builder()
        properties.forEach { property -> builder.addProperty(property.key, property.value) }
        return defaultUnleash.isEnabled(toggleId, builder.build())
    }

    override fun destroy() {
        // Spring trigger denne ved shutdown. Gjøres for å unngå at unleash fortsetter å gjøre kall ut
        defaultUnleash.shutdown()
    }
}
