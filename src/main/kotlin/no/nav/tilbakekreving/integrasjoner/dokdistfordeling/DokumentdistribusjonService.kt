package no.nav.tilbakekreving.integrasjoner.dokdistfordeling

import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostResponseTo
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import java.util.UUID

interface DokumentdistribusjonService {
    suspend fun sendBrev(
        journalpostId: String,
        fagsystem: FagsystemDTO,
        behandlingId: UUID,
        fagsystemId: String,
        logContext: SecureLog.Context,
    ): DistribuerJournalpostResponseTo?
}
