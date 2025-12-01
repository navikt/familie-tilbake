package no.nav.tilbakekreving.integrasjoner.dokarkiv

import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostResponse
import no.nav.tilbakekreving.kontrakter.ytelse.DokarkivFagsaksystem
import no.nav.tilbakekreving.kontrakter.ytelse.Tema
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.DokumentKlasse
import java.util.UUID

interface DokarkivClient {
    fun opprettOgSendJournalpostRequest(
        arkiverDokument: ArkiverDokumentRequest,
        fagsaksystem: DokarkivFagsaksystem,
        brevkode: String,
        tema: Tema,
        dokuemntkategori: DokumentKlasse,
        behandlingId: UUID,
    ): OpprettJournalpostResponse
}
