package no.nav.familie.tilbake.config

import io.getunleash.strategy.Strategy
import no.nav.familie.unleash.UnleashService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Configuration
class FeatureToggleConfig(
    @Value("\${NAIS_CLUSTER_NAME}") private val clusterName: String,
) {
    @Bean
    fun strategies(): List<Strategy> = listOf(ByClusterStrategy(clusterName))

    companion object {
        const val KAN_SE_HISTORISKE_VURDERINGER = "familie-tilbake.se-historiske-vurderinger"

        const val SAKSBEHANDLER_KAN_RESETTE_BEHANDLING = "familie-tilbake-frontend.saksbehandler.kan.resette.behandling"

        const val TIDLIGERE_BESLUTTER = "familie-tilbake.tidligere-beslutter"
    }
}

@Service
@Profile("!integrasjonstest")
class FeatureToggleService(
    val unleashService: UnleashService,
) {
    fun isEnabled(toggleId: String): Boolean = unleashService.isEnabled(toggleId, false)

    fun isEnabled(
        toggleId: String,
        defaultValue: Boolean,
    ): Boolean = unleashService.isEnabled(toggleId, defaultValue)
}

class ByClusterStrategy(
    private val clusterName: String,
) : Strategy {
    override fun isEnabled(parameters: MutableMap<String, String>): Boolean {
        if (parameters.isEmpty()) return false
        return parameters["cluster"]?.contains(clusterName) ?: false
    }

    override fun getName(): String = "byCluster"
}
