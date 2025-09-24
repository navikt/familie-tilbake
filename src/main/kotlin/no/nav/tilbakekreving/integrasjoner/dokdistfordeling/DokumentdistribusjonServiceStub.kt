package no.nav.tilbakekreving.integrasjoner.dokdistfordeling

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostRequestTo
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostResponseTo
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Profile("e2e", "local", "integrasjonstest")
@Service
class DokumentdistribusjonServiceStub : DokumentdistribusjonService {
    override suspend fun sendBrev(tilbakekreving: Tilbakekreving, req: DistribuerJournalpostRequestTo): DistribuerJournalpostResponseTo? {
        return null
    }
}
