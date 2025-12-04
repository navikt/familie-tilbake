package no.nav.tilbakekreving.integrasjoner.dokumenthenting.domain

import no.nav.familie.tilbake.kontrakter.journalpost.Journalpost

data class SafHentJournalpostResponse(
    val data: SafJournalpostData?,
)

data class SafJournalpostData(
    val dokumentoversiktBruker: DokumentoversiktBruker?,
)

data class DokumentoversiktBruker(
    val journalposter: List<Journalpost>,
)
