package no.nav.tilbakekreving.auth

interface VedtaksdokumentTilgang {
    fun valider(token: String?, tilgang: Tilgang)

    enum class Tilgang(
        val rolle: String,
        val feilmelding: String,
        val frontendFeilmelding: String,
    ) {
        TVANGSGRUNNLAG_LES(
            rolle = "tvangsgrunnlag.read",
            feilmelding = "Klienten har ikke tilgang til vedtaksdokumenter",
            frontendFeilmelding = "Ingen tilgang til vedtaksdokumenter",
        ),
    }
}
