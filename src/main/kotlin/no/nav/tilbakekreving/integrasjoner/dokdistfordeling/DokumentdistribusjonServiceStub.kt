package no.nav.tilbakekreving.integrasjoner.dokdistfordeling

import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostResponseTo
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.UUID

@Profile("e2e", "local", "integrasjonstest")
@Service
class DokumentdistribusjonServiceStub : DokumentdistribusjonService {
    override suspend fun sendBrev(
        journalpostId: String,
        fagsystem: FagsystemDTO,
        behandlingId: UUID,
        fagsystemId: String,
        logContext: SecureLog.Context,
    ): DistribuerJournalpostResponseTo? {
        return null
    }
}
