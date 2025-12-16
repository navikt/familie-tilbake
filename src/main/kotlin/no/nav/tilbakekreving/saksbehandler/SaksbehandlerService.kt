package no.nav.tilbakekreving.saksbehandler

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
        if (id == ID_VEDTAKSLØSNINGEN) {
            return Saksbehandler(
                navIdent = ID_VEDTAKSLØSNINGEN,
                fornavn = "Vedtaksløsning",
                etternavn = "Nav",
                enhet = "9999",
                enhetsnavn = null,
            )
        }
        val entraBruker = entraProxyClient.hentSaksbehandler(id)

        return Saksbehandler(
            navIdent = entraBruker.navIdent,
            fornavn = entraBruker.fornavn,
            etternavn = entraBruker.etternavn,
            enhet = entraBruker.enhet.enhetnummer,
            enhetsnavn = entraBruker.enhet.navn,
        )
    }

    companion object {
        const val ID_VEDTAKSLØSNINGEN = "VL"
    }
}
