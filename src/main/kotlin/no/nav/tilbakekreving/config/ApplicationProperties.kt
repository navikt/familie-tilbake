package no.nav.tilbakekreving.config

import no.nav.tilbakekreving.integrasjoner.dokarkiv.config.DokarkivConfig
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.config.DokdistConfig
import no.tilbakekreving.integrasjoner.arbeidsforhold.EregClient
import no.tilbakekreving.integrasjoner.dokument.saf.SafClient
import no.tilbakekreving.integrasjoner.entraProxy.EntraProxyClient
import no.tilbakekreving.integrasjoner.norg2.Norg2Client
import no.tilbakekreving.integrasjoner.persontilgang.PersontilgangService
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("tilbakekreving")
data class ApplicationProperties(
    val toggles: Toggles = Toggles(),
    val kravgrunnlag: List<String> = emptyList(),
    val tilgangsstyring: Tilgangsstyring,
    val tokenExchange: TokenExchangeService.Companion.Config,
    val tilgangsmaskinen: PersontilgangService.Companion.Config,
    val bigQuery: BigQueryProperties,
    val frontendUrl: String,
    val kravgrunnlagMapping: Map<String, String> = emptyMap(),
    val dokarkiv: DokarkivConfig,
    val dokdist: DokdistConfig,
    val saf: SafClient.Companion.Config,
    val norg2: Norg2Client.Companion.Config,
    val eregServices: EregClient.Companion.Config,
    val entraProxy: EntraProxyClient.Companion.Config,
)
