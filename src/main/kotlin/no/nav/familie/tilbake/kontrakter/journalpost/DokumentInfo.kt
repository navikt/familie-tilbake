package no.nav.familie.tilbake.kontrakter.journalpost

data class DokumentInfo(
    val dokumentInfoId: String,
    val tittel: String? = null,
    val brevkode: String? = null,
    val logiskeVedlegg: List<LogiskVedlegg>? = null,
)
