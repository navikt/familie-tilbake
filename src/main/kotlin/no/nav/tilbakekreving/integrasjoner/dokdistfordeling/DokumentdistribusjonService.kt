package no.nav.tilbakekreving.integrasjoner.dokdistfordeling

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostRequestTo
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostResponseTo

interface DokumentdistribusjonService {
    suspend fun sendBrev(
        tilbakekreving: Tilbakekreving,
        req: DistribuerJournalpostRequestTo,
    ): DistribuerJournalpostResponseTo?
}
