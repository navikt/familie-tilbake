package no.nav.tilbakekreving.integrasjoner.dokumenthenting

import java.util.UUID

class SafClientStub() : SafClient {
    override fun hentDokument(
        behandlingId: UUID,
        journalpostId: String,
        dokumentInfoId: String,
    ): ByteArray {
        return ByteArray(0)
    }
}
