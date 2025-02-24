package no.nav.familie.tilbake.kontrakter.oppgave

data class MappeDto(
    val id: Int,
    val navn: String,
    val enhetsnr: String,
    val tema: String? = null,
)
