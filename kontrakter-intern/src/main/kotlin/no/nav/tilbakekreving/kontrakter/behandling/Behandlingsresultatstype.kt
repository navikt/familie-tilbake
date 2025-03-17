package no.nav.tilbakekreving.kontrakter.behandling

enum class Behandlingsresultatstype(
    val navn: String,
) {
    IKKE_FASTSATT("Ikke fastsatt"),
    HENLAGT_FEILOPPRETTET("Henlagt, søknaden er feilopprettet"),
    HENLAGT_FEILOPPRETTET_MED_BREV("Feilaktig opprettet - med henleggelsesbrev"),
    HENLAGT_FEILOPPRETTET_UTEN_BREV("Feilaktig opprettet - uten henleggelsesbrev"),
    HENLAGT_KRAVGRUNNLAG_NULLSTILT("Kravgrunnlaget er nullstilt"),
    HENLAGT_TEKNISK_VEDLIKEHOLD("Teknisk vedlikehold"),
    HENLAGT_MANGLENDE_KRAVGRUNNLAG("Uten kravgrunnlag i 8 uker"),
    HENLAGT("Henlagt"), // kun brukes i frontend

    INGEN_TILBAKEBETALING("Ingen tilbakebetaling"),
    DELVIS_TILBAKEBETALING("Delvis tilbakebetaling"),
    FULL_TILBAKEBETALING("Full tilbakebetaling"),
}
