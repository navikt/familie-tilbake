package no.nav.tilbakekreving.integrasjoner.dokarkiv

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostRequest
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostResponse
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Profile("e2e", "local", "integrasjonstest")
@Service
class DokarkivServiceStub() : DokarkivService {
    override suspend fun lagJournalpost(tilbakekreving: Tilbakekreving, request: OpprettJournalpostRequest, ferdigstill: Boolean): OpprettJournalpostResponse {
        TODO("Not yet implemented")
    }

    override fun journalføringTest(tilbakekreving: Tilbakekreving): OpprettJournalpostResponse {
        TODO("Not yet implemented")
    }
}
