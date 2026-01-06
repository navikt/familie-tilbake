package no.nav.tilbakekreving.saksbehandler

import no.nav.familie.tilbake.config.Constants.BRUKER_ID_VEDTAKSLØSNINGEN
import no.nav.familie.tilbake.kontrakter.saksbehandler.Saksbehandler
import no.tilbakekreving.integrasjoner.entraProxy.EntraProxyClient
import org.springframework.stereotype.Service

@Service
class SaksbehandlerService(
    private val entraProxyClient: EntraProxyClient,
) {
    fun hentSaksbehandler(
        id: String,
    ): Saksbehandler {
        if (id == BRUKER_ID_VEDTAKSLØSNINGEN) {
            return Saksbehandler(
                navIdent = BRUKER_ID_VEDTAKSLØSNINGEN,
                fornavn = "Vedtaksløsning",
                etternavn = "Nav",
                enhet = "9999",
            )
        }
        val entraBruker = entraProxyClient.hentSaksbehandler(id)

        return Saksbehandler(
            navIdent = entraBruker.navIdent,
            fornavn = entraBruker.fornavn,
            etternavn = entraBruker.etternavn,
            enhet = entraBruker.enhet.enhetnummer,
        )
    }
}
