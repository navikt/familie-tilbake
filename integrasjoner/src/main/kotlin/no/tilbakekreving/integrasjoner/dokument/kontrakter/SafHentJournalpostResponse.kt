package no.tilbakekreving.integrasjoner.dokument.kontrakter

data class SafHentJournalpostResponse(
    val data: SafJournalpostData?,
)

data class SafJournalpostData(
    val dokumentoversiktBruker: DokumentoversiktBruker?,
)

data class DokumentoversiktBruker(
    val journalposter: List<JournalpostResponse>,
)
