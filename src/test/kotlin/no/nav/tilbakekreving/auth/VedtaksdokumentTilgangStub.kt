package no.nav.tilbakekreving.auth

import no.nav.tilbakekreving.auth.VedtaksdokumentTilgang.Resultat
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@Primary
class VedtaksdokumentTilgangStub : VedtaksdokumentTilgang {
    override fun vurder(token: String?): Resultat = when (token) {
        GODKJENT_TOKEN -> Resultat.GODKJENT
        UTEN_ROLLE_TOKEN -> Resultat.MANGLER_ROLLE
        else -> Resultat.UGYLDIG_TOKEN
    }

    companion object {
        const val GODKJENT_TOKEN = "godkjent-token"
        const val UTEN_ROLLE_TOKEN = "uten-rolle-token"
    }
}
