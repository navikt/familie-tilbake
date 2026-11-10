package no.nav.tilbakekreving.auth

interface VedtaksdokumentTilgang {
    fun vurder(token: String?): Resultat

    enum class Resultat {
        GODKJENT,
        UGYLDIG_TOKEN,
        MANGLER_ROLLE,
    }
}
