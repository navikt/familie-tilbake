package no.nav.tilbakekreving.saksbehandler

import no.nav.familie.tilbake.kontrakter.saksbehandler.Saksbehandler
import no.tilbakekreving.integrasjoner.azure.AzureGraphClient
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaksbehandlerService(
    private val azureGraphClient: AzureGraphClient,
) {
    private val lengdeNavIdent = 7

    fun hentSaksbehandler(id: String): Saksbehandler {
        if (id == ID_VEDTAKSLØSNINGEN) {
            return Saksbehandler(
                azureId = UUID.randomUUID(),
                navIdent = ID_VEDTAKSLØSNINGEN,
                fornavn = "Vedtaksløsning",
                etternavn = "Nav",
                enhet = "9999",
                enhetsnavn = null,
            )
        }

        val azureAdBruker =
            if (id.length == lengdeNavIdent) {
                val azureAdBrukere = azureGraphClient.finnSaksbehandler(id)

                if (azureAdBrukere.value.size != 1) {
                    error("Feil ved søk. Oppslag på navIdent $id returnerte ${azureAdBrukere.value.size} forekomster.")
                }
                azureAdBrukere.value.first()
            } else {
                azureGraphClient.hentSaksbehandler(id)
            }

        return Saksbehandler(
            azureId = azureAdBruker.id,
            navIdent = azureAdBruker.onPremisesSamAccountName,
            fornavn = azureAdBruker.givenName,
            etternavn = azureAdBruker.surname,
            enhet = azureAdBruker.streetAddress,
            enhetsnavn = azureAdBruker.city,
        )
    }

    companion object {
        const val ID_VEDTAKSLØSNINGEN = "VL"
    }
}
