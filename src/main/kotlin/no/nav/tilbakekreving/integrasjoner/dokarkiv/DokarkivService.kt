package no.nav.tilbakekreving.integrasjoner.dokarkiv

import no.nav.familie.tilbake.log.SecureLog
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostRequest
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostResponse

interface DokarkivService {
    suspend fun lagJournalpost(
        request: OpprettJournalpostRequest,
        ferdigstill: Boolean,
        behandlingId: String,
        eksternFagsakId: String,
        logContext: SecureLog.Context,
    ): OpprettJournalpostResponse?

    suspend fun journalførVarselbrev(
        token: JwtToken,
        varselbrevBehov: VarselbrevBehov,
        logContext: SecureLog.Context,
    ): OpprettJournalpostResponse?
}
