package no.nav.tilbakekreving.integrasjoner.azure

import no.nav.tilbakekreving.integrasjoner.azure.domain.AzureAdBrukere

class AzureGraphClientStub(): AzureGraphClient {
    override fun finnSaksbehandler(): AzureAdBrukere {
        return AzureAdBrukere(listOf())
    }
}