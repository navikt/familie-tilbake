package no.nav.tilbakekreving.integrasjoner

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.config.BigQueryProperties
import no.nav.tilbakekreving.config.Tilgangsstyring
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.config.DokdistConfig
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.tilbakekreving.integrasjoner.persontilgang.PersontilgangService
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService

fun tokenValidationContextWith(value: String): TokenValidationContext = mockk {
    every { firstValidToken } returns mockk {
        every { encodedToken } returns value
    }
}

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
        dokdist = DokdistConfig(
            baseUrl = "http://dokdist",
            scope = "api://dokdist/.default",
        ),
    )
}
