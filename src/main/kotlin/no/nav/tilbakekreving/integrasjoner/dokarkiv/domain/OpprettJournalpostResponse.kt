package no.nav.tilbakekreving.integrasjoner.dokarkiv.domain

class OpprettJournalpostResponse(
    val journalpostId: String? = null,
    val melding: String? = null,
    val journalpostferdigstilt: Boolean? = false,
    val dokumenter: List<DokumentInfo>? = null,
)
