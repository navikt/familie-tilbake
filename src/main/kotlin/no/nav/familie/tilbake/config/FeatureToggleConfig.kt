package no.nav.familie.tilbake.config

import io.getunleash.strategy.Strategy
import no.nav.familie.unleash.DefaultUnleashService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Configuration
class FeatureToggleConfig(
    @Value("\${UNLEASH_SERVER_API_URL}") private val apiUrl: String,
    @Value("\${UNLEASH_SERVER_API_TOKEN}") private val apiToken: String,
    @Value("\${NAIS_APP_NAME}") private val appName: String,
    @Value("\${NAIS_CLUSTER_NAME}") private val clusterName: String,

) {

    @Bean
    fun strategies(): List<Strategy> {
        return listOf(ByClusterStrategy(clusterName))
    }

    @Bean
    fun defaultUnleashService(strategies: List<Strategy>): DefaultUnleashService {
        return DefaultUnleashService(apiUrl, apiToken, appName, strategies)
    }

    companion object {

        const val KAN_OPPRETTE_BEH_MED_EKSTERNID_SOM_HAR_AVSLUTTET_TBK =
            "familie-tilbake.beh.kanopprettes.eksternid.avsluttet.tilbakekreving"

        const val SETT_PRIORITET_PÅ_OPPGAVER = "familie.tilbake.prioritet-oppgaver"

        const val DISTRIBUER_TIL_MANUELLE_BREVMOTTAKERE = "familie-tilbake.manuelle-brev"

        const val KONSOLIDERT_HÅNDTERING_AV_BREVMOTTAKERE = "familie-tilbake.konsolidert-brevdistribusjon"

        const val OVERSTYR_DELVILS_TILBAKEKREVING_TIL_FULL_TILBAKEKREVING = "familie-tilbake.overstyr-delvis-hvis-full"

        const val BRUK_6_DESIMALER_I_SKATTEBEREGNING = "familie-tilbake.bruk-seks-desimaler-skatt"

        const val IKKE_VALIDER_SÆRLIG_GRUNNET_ANNET_FRITEKST =
            "familie-tilbake.ikke-valider-saerlig-grunnet-annet-fritekst"
    }
}

@Service
@Profile("!integrasjonstest")
class FeatureToggleService(val defaultUnleashService: DefaultUnleashService) {

    fun isEnabled(toggleId: String): Boolean {
        return defaultUnleashService.isEnabled(toggleId, false)
    }

    fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
        return defaultUnleashService.isEnabled(toggleId, defaultValue)
    }
}

/**
 * TODO : Fjern etter testing er utført.
 */
@RestController
@RequestMapping(path = ["/api/featuretoggle"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Unprotected
class FeatureToggleController(private val featureToggleService: FeatureToggleService) {

    private val funksjonsbrytere: Set<String> = setOf(
        FeatureToggleConfig.BRUK_6_DESIMALER_I_SKATTEBEREGNING,
        FeatureToggleConfig.DISTRIBUER_TIL_MANUELLE_BREVMOTTAKERE,
        FeatureToggleConfig.KONSOLIDERT_HÅNDTERING_AV_BREVMOTTAKERE,
        FeatureToggleConfig.KAN_OPPRETTE_BEH_MED_EKSTERNID_SOM_HAR_AVSLUTTET_TBK,
        FeatureToggleConfig.OVERSTYR_DELVILS_TILBAKEKREVING_TIL_FULL_TILBAKEKREVING,
        FeatureToggleConfig.IKKE_VALIDER_SÆRLIG_GRUNNET_ANNET_FRITEKST,
    )

    @GetMapping
    fun sjekkAlle(): Map<String, Boolean> {
        return funksjonsbrytere.associate { it to featureToggleService.isEnabled(it) }
    }
}

class ByClusterStrategy(private val clusterName: String) : Strategy {

    override fun isEnabled(parameters: MutableMap<String, String>): Boolean {
        if (parameters.isEmpty()) return false
        return parameters["cluster"]?.contains(clusterName) ?: false
    }

    override fun getName(): String = "byCluster"
}