package no.nav.tilbakekreving.config

import no.tilbakekreving.integrasjoner.persontilgang.PersontilgangService
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("tilbakekreving")
data class ApplicationProperties(
    val toggles: Toggles = Toggles(
        nyModellEnabled = false,
    ),
    val kravgrunnlag: List<String> = emptyList(),
    val tilgangsstyring: Tilgangsstyring,
    val tokenExchange: TokenExchangeService.Companion.Config,
    val tilgangsmaskinen: PersontilgangService.Companion.Config,
)
