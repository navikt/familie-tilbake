package no.nav.tilbakekreving.integrasjoner.dokdistfordeling

import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstidspunkt
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstype
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.AdresseTo
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostResponse
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import java.util.UUID

interface DokdistClient {
    fun brevTilUtsending(
        behandlingId: UUID,
        journalpostId: String,
        fagsystem: FagsystemDTO,
        distribusjonstype: Distribusjonstype,
        distribusjonstidspunkt: Distribusjonstidspunkt,
        adresse: AdresseTo?,
        logContext: SecureLog.Context,
    ): DistribuerJournalpostResponse
}
