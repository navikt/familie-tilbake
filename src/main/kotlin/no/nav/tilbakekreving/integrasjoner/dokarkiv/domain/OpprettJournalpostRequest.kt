package no.nav.tilbakekreving.integrasjoner.dokarkiv.domain

import no.nav.familie.tilbake.kontrakter.dokarkiv.AvsenderMottaker

data class OpprettJournalpostRequest(
    val journalpostType: JournalpostType?,
    val avsenderMottaker: AvsenderMottaker?,
    val bruker: DokarkivBruker?,
    val tema: String?,
    val behandlingstema: String?,
    val tittel: String?,
    val kanal: String?,
    val journalfoerendeEnhet: String?,
    val eksternReferanseId: String?,
    val sak: Sak?,
    val dokumenter: List<ArkivDokument>,
)
