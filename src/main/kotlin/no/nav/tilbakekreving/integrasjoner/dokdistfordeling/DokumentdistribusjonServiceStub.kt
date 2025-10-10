package no.nav.tilbakekreving.integrasjoner.dokdistfordeling

import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostRequest
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostResponse
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.UUID

@Profile("e2e", "local", "integrasjonstest")
@Service
class DokumentdistribusjonServiceStub : DokumentdistribusjonService {
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
