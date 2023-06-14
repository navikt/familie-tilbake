package no.nav.familie.tilbake.config

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.UnleashContextProvider
import io.getunleash.strategy.GradualRolloutRandomStrategy
import io.getunleash.strategy.Strategy
import io.getunleash.util.UnleashConfig
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import java.net.URI

@ConfigurationProperties("funksjonsbrytere")
@ConstructorBinding
class FeatureToggleConfig(
    private val enabled: Boolean,
    val unleash: Unleash
) {

    @ConstructorBinding
    data class Unleash(
        val uri: URI,
        val cluster: String,
        val applicationName: String
    )

    @Bean
    @Profile("!integrasjonstest")
    fun featureToggle(): FeatureToggleService =
        if (enabled) {
            lagUnleashFeatureToggleService()
        } else {
            logger.warn(
                "Unleash feature toggle er skrudd AV. Gir standardoppførsel for alle funksjonsbrytere, dvs 'false'"
            )
            lagDummyFeatureToggleService()
        }

    private fun lagUnleashFeatureToggleService(): FeatureToggleService {
        val defaultUnleash = DefaultUnleash(
            UnleashConfig.builder()
                .appName(unleash.applicationName)
                .unleashAPI(unleash.uri)
                .unleashContextProvider(lagUnleashContextProvider())
                .build(),
            ByClusterStrategy(unleash.cluster),
            GradualRolloutRandomStrategy()
        )

        return object : FeatureToggleService {
            override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
                return defaultUnleash.isEnabled(toggleId, defaultValue)
            }
        }
    }

    private fun lagUnleashContextProvider(): UnleashContextProvider {
        return UnleashContextProvider {
            UnleashContext.builder()
                .appName(unleash.applicationName)
                .build()
        }
    }

    class ByClusterStrategy(private val clusterName: String) : Strategy {

        override fun isEnabled(parameters: MutableMap<String, String>): Boolean {
            if (parameters.isEmpty()) return false
            return parameters["cluster"]?.contains(clusterName) ?: false
        }

        override fun getName(): String = "byCluster"
    }

    private fun lagDummyFeatureToggleService(): FeatureToggleService {
        return object : FeatureToggleService {
            override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
                return System.getenv(toggleId).run { toBoolean() } || defaultValue
            }
        }
    }

    companion object {

        const val KAN_OPPRETTE_BEH_MED_EKSTERNID_SOM_HAR_AVSLUTTET_TBK =
            "familie-tilbake.beh.kanopprettes.eksternid.avsluttet.tilbakekreving"

        const val SETT_PRIORITET_PÅ_OPPGAVER = "familie.tilbake.prioritet-oppgaver"

        const val DISTRIBUER_TIL_MANUELLE_BREVMOTTAKERE = "familie-tilbake.manuelle-brev"

        const val KONSOLIDERT_HÅNDTERING_AV_BREVMOTTAKERE = "familie-tilbake.konsolidert-brevdistribusjon"

        const val OVERSTYR_DELVILS_TILBAKEKREVING_TIL_FULL_TILBAKEKREVING = "familie-tilbake-overstyr-delvis-hvis-full"

        private val logger = LoggerFactory.getLogger(FeatureToggleConfig::class.java)
    }
}

interface FeatureToggleService {

    fun isEnabled(toggleId: String): Boolean {
        return isEnabled(toggleId, false)
    }

    fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean
}
