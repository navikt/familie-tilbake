package no.nav.tilbakekreving.saksbehandler

import no.tilbakekreving.integrasjoner.entraProxy.EntraProxyClient
import no.tilbakekreving.integrasjoner.entraProxy.kontrakter.Enhet
import no.tilbakekreving.integrasjoner.entraProxy.kontrakter.Saksbehandler

class EntraProxyClientStub : EntraProxyClient {
    override fun hentSaksbehandler(id: String): Saksbehandler {
        return Saksbehandler(
            "bb1234",
            "",
            "Bob",
            "Burger",
            "",
            Enhet("0425", "jnkmmk"),
            "",
        )
    }
}
