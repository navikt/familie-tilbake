package no.nav.tilbakekreving.integrasjoner.dokarkiv.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

// dokarkiv koden returnerer også journalstatus men det er @Deprecated. Så ignorer ukjente her.
@JsonIgnoreProperties(ignoreUnknown = true)
data class OpprettJournalpostResponse(
    val journalpostId: String? = null,
    val melding: String? = null,
    val journalpostferdigstilt: Boolean? = false,
    val dokumenter: List<DokumentInfo>? = null,
)
