package no.nav.tilbakekreving.integrasjoner.dokdistfordeling

import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostRequest
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostResponse
import java.util.UUID

class DokdistClientStub : DokdistClient {
    override suspend fun sendBrev(
        request: DistribuerJournalpostRequest,
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ): DistribuerJournalpostResponse? {
        return null
    }

    override fun brevTilUtsending(
        behov: VarselbrevBehov,
        journalpostId: String,
        logContext: SecureLog.Context,
    ): DistribuerJournalpostResponse? {
        return null
    }
}
