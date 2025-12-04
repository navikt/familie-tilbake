package no.nav.tilbakekreving.integrasjoner.dokumenthenting

import java.util.UUID

interface SafClient {
    fun hentDokument(
        behandlingId: UUID,
        journalpostId: String,
        dokumentInfoId: String,
    ): ByteArray
}
