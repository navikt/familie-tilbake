package no.nav.tilbakekreving.integrasjoner.azure

import no.nav.tilbakekreving.integrasjoner.azure.domain.AzureAdBrukere

interface AzureGraphClient {
    suspend fun finnSaksbehandler(navIdent: String): AzureAdBrukere
}