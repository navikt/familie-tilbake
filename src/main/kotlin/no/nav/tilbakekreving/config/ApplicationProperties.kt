package no.nav.tilbakekreving.config

import no.nav.tilbakekreving.integrasjoner.dokarkiv.config.DokarkivConfig
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.config.DokdistConfig
import no.tilbakekreving.integrasjoner.persontilgang.PersontilgangService
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("tilbakekreving")
data class ApplicationProperties(
    val toggles: Toggles = Toggles(
        nyModellEnabled = false,
        sendVarselbrev = false
    ),
    val kravgrunnlag: List<String> = emptyList(),
    val tilgangsstyring: Tilgangsstyring,
    val tokenExchange: TokenExchangeService.Companion.Config,
    val tilgangsmaskinen: PersontilgangService.Companion.Config,
    val bigQuery: BigQueryProperties,
    val frontendUrl: String,
    val kravgrunnlagMapping: Map<String, String> = emptyMap(),
    val dokarkiv: DokarkivConfig,
    val dokdist: DokdistConfig,
)
