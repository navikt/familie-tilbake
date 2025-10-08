package no.nav.tilbakekreving.integrasjoner.dokarkiv.domain

import no.nav.familie.tilbake.kontrakter.dokarkiv.AvsenderMottaker

data class OpprettJournalpostRequest(
    val journalpostType: JournalpostType? = null,
    val avsenderMottaker: AvsenderMottaker? = null,
    val bruker: DokarkivBruker? = null,
    val tema: String? = null,
    val behandlingstema: String? = null,
    val tittel: String? = null,
    val kanal: String? = null,
    val journalfoerendeEnhet: String? = null,
    val eksternReferanseId: String? = null,
    val sak: Sak? = null,
    val dokumenter: List<ArkivDokument> = ArrayList(),
)
