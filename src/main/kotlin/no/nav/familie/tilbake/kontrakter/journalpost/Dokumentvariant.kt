package no.nav.familie.tilbake.kontrakter.journalpost

data class Dokumentvariant(
    val variantformat: Dokumentvariantformat,
    val filnavn: String? = null,
    val saksbehandlerHarTilgang: Boolean,
)
