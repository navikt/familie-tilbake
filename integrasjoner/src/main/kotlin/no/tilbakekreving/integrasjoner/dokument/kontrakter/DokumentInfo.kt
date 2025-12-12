package no.tilbakekreving.integrasjoner.dokument.kontrakter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DokumentInfo(
    val dokumentInfoId: String,
    val tittel: String? = null,
    val brevkode: String? = null,
    val logiskeVedlegg: List<LogiskVedlegg>? = null,
)

data class LogiskVedlegg(
    val logiskVedleggId: String,
    val tittel: String,
)
