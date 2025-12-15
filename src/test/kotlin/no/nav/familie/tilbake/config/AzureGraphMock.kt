package no.nav.familie.tilbake.config

import no.tilbakekreving.integrasjoner.azure.AzureGraphClient
import no.tilbakekreving.integrasjoner.azure.kontrakter.AzureAdBruker
import no.tilbakekreving.integrasjoner.azure.kontrakter.AzureAdBrukere
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Primary
class AzureGraphMock : AzureGraphClient {
    override fun finnSaksbehandler(navIdent: String): AzureAdBrukere {
        return AzureAdBrukere(listOf())
    }

    override fun hentSaksbehandler(id: String): AzureAdBruker {
        return AzureAdBruker(UUID.randomUUID(), "bb1234", "", "Bob", "Burger", "0425", "")
    }
}
