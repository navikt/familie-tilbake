package no.nav.tilbakekreving.integrasjoner.dokarkiv

import no.nav.familie.tilbake.kontrakter.dokarkiv.DokumentInfo
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostResponse
import no.nav.tilbakekreving.kontrakter.ytelse.DokarkivFagsaksystem
import no.nav.tilbakekreving.kontrakter.ytelse.Tema
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.DokumentKlasse
import org.springframework.context.annotation.Profile
import java.util.UUID

@Profile("e2e", "local", "integrasjonstest")
class DokarkivClientStub() : DokarkivClient {
    override fun opprettOgSendJournalpostRequest(
        arkiverDokument: ArkiverDokumentRequest,
        fagsaksystem: DokarkivFagsaksystem,
        brevkode: String,
        tema: Tema,
        dokuemntkategori: DokumentKlasse,
        behandlingId: UUID,
    ): OpprettJournalpostResponse {
        return OpprettJournalpostResponse(journalpostId = "-1", null, null, listOf(DokumentInfo("-2")))
    }
}
