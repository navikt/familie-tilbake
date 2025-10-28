package no.nav.tilbakekreving.integrasjoner.dokarkiv

import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostRequest
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostResponse
import org.springframework.context.annotation.Profile

@Profile("e2e", "local", "integrasjonstest")
class DokarkivClientStub() : DokarkivClient {
    override suspend fun lagJournalpost(
        request: OpprettJournalpostRequest,
        ferdigstill: Boolean,
        behandlingId: String,
        eksternFagsakId: String,
        logContext: SecureLog.Context,
    ): OpprettJournalpostResponse {
        return OpprettJournalpostResponse(journalpostId = "-1", null, null, null)
    }

    override fun journalførVarselbrev(
        varselbrevBehov: VarselbrevBehov,
        logContext: SecureLog.Context,
    ): OpprettJournalpostResponse {
        return OpprettJournalpostResponse(journalpostId = "-1", null, null, null)
    }
}
