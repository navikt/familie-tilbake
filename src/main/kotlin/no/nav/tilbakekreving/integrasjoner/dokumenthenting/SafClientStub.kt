package no.nav.tilbakekreving.integrasjoner.dokumenthenting

import no.nav.familie.tilbake.kontrakter.journalpost.Journalpost
import no.nav.tilbakekreving.Tilbakekreving
import java.util.UUID

class SafClientStub() : SafClient {
    override fun hentDokument(
        behandlingId: UUID,
        journalpostId: String,
        dokumentInfoId: String,
    ): ByteArray {
        return ByteArray(0)
    }

    override fun hentJournalposter(tilbakekreving: Tilbakekreving): List<Journalpost> {
        return listOf()
    }
}
