package no.nav.tilbakekreving.saksbehandler

import no.tilbakekreving.integrasjoner.entraProxy.EntraProxyClient
import no.tilbakekreving.integrasjoner.entraProxy.kontrakter.Enhet
import no.tilbakekreving.integrasjoner.entraProxy.kontrakter.Saksbehandler

class EntraProxyClientStub : EntraProxyClient {
    override fun hentSaksbehandler(id: String): Saksbehandler {
        return Saksbehandler(
            navIdent = "bb1234",
            visningNavn = "",
            fornavn = "Bob",
            etternavn = "Burger",
            epost = "",
            enhet = Enhet(enhetnummer = "0425"),
        )
    }
}
