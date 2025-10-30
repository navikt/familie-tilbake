package no.nav.tilbakekreving

import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.config.BigQueryProperties
import no.nav.tilbakekreving.config.Tilgangsstyring
import no.nav.tilbakekreving.integrasjoner.dokarkiv.config.DokarkivConfig
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.config.DokdistConfig
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.tilbakekreving.integrasjoner.persontilgang.PersontilgangService
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService

fun applicationProps(): ApplicationProperties {
    return ApplicationProperties(
        tilgangsstyring = Tilgangsstyring(
            grupper = mapOf(
                FagsystemDTO.TS to mapOf(
                    Behandlerrolle.SYSTEM to "",
                ),
            ),
            forvalterGruppe = "",
        ),
        tokenExchange = TokenExchangeService.Companion.Config(
            tokenEndpoint = "tokenEndpoint",
            tokenExchangeEndpoint = "tokenExchangeEndpoint",
            tokenIntrospectionEndpoint = "tokenIntrospectionEndpoint",
        ),
        tilgangsmaskinen = PersontilgangService.Companion.Config(
            baseUrl = "http://tilgangsmaskinen",
            scope = "api://Scope",
        ),
        bigQuery = BigQueryProperties(
            prosjektId = "test-project",
            dataset = "test-dataset",
        ),
        frontendUrl = "http://frontend",
        dokarkiv = DokarkivConfig(
            baseUrl = "http://dokarkiv",
            scope = "api://dokarkiv/.default",
        ),
        dokdist = DokdistConfig(
            baseUrl = "http://dokdist",
            scope = "api://dokdist/.default",
        ),
    )
}
