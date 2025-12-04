package no.nav.familie.tilbake.kontrakter.journalpost

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DokumentInfo(
    val dokumentInfoId: String,
    val tittel: String? = null,
    val brevkode: String? = null,
    val logiskeVedlegg: List<LogiskVedlegg>? = null,
)
