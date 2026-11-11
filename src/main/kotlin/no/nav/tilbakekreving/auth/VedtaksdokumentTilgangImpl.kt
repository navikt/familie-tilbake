package no.nav.tilbakekreving.auth

import kotlinx.coroutines.runBlocking
import no.nav.tilbakekreving.auth.VedtaksdokumentTilgang.Resultat
import no.nav.tilbakekreving.auth.VedtaksdokumentTilgang.Tilgang
import no.nav.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.springframework.stereotype.Component

@Component
class VedtaksdokumentTilgangImpl(
    private val tokenExchangeService: TokenExchangeService,
) : VedtaksdokumentTilgang {
    override fun vurder(token: String?, tilgang: Tilgang): Resultat {
        if (token.isNullOrBlank()) return Resultat.UGYLDIG_TOKEN

        val claims = runBlocking { tokenExchangeService.introspectToken(token) }
        if (!claims.active) return Resultat.UGYLDIG_TOKEN

        return if (claims.roles?.contains(tilgang.rolle) == true) Resultat.GODKJENT else Resultat.MANGLER_ROLLE
    }
}
