package no.nav.tilbakekreving.integrasjoner.dokarkiv

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostRequest
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostResponse

interface DokarkivService {
    suspend fun lagJournalpost(
        tilbakekreving: Tilbakekreving,
        request: OpprettJournalpostRequest,
        ferdigstill: Boolean,
    ): OpprettJournalpostResponse

    fun journalføringTest(tilbakekreving: Tilbakekreving): OpprettJournalpostResponse
}
