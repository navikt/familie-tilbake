package no.nav.tilbakekreving

import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.config.BigQueryProperties
import no.nav.tilbakekreving.config.Tilgangsstyring
import no.nav.tilbakekreving.integrasjoner.arbeidsforhold.EregClient
import no.nav.tilbakekreving.integrasjoner.dokarkiv.config.DokarkivConfig
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.config.DokdistConfig
import no.nav.tilbakekreving.integrasjoner.dokument.saf.SafClient
import no.nav.tilbakekreving.integrasjoner.entraProxy.EntraProxyClient
import no.nav.tilbakekreving.integrasjoner.norg2.Norg2Client
import no.nav.tilbakekreving.integrasjoner.oppdrag.OppdragRestClient
import no.nav.tilbakekreving.integrasjoner.pdfGen.PdfGenClient
import no.nav.tilbakekreving.integrasjoner.persontilgang.PersontilgangService
import no.nav.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO

fun applicationProps(): ApplicationProperties {
    return ApplicationProperties(
        tilgangsstyring = Tilgangsstyring(
            grupper = mapOf(
                FagsystemDTO.TS to mapOf(
                    Behandlerrolle.SYSTEM to listOf(""),
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
        saf = SafClient.Companion.Config(
            baseUrl = "http://saf",
            scope = "api://saf/.default",
        ),
        norg2 = Norg2Client.Companion.Config(
            baseUrl = "http://norg2",
            scope = "api://norg2/.default",
        ),
        eregServices = EregClient.Companion.Config(
            baseUrl = "http://eregServices",
            scope = "api://ereg-services/.default",
        ),
        entraProxy = EntraProxyClient.Companion.Config(
            baseUrl = "http://azure",
            scope = "api://azure/.default",
        ),
        tilbakekrevingPdf = PdfGenClient.Companion.Config(
            baseUrl = "http://pdfGen",
        ),
        sokosOs = OppdragRestClient.Companion.Config(
            baseUrl = "http://sokos-os-ekstern-api",
            scope = "api://sokos-os-ekstern-api/.default",
        ),
    )
}
