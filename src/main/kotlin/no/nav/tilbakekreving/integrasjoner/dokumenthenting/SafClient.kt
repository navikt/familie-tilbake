package no.nav.tilbakekreving.integrasjoner.dokumenthenting

import no.nav.familie.tilbake.kontrakter.journalpost.Journalpost
import no.nav.tilbakekreving.Tilbakekreving
import java.util.UUID

interface SafClient {
    fun hentDokument(
        behandlingId: UUID,
        journalpostId: String,
        dokumentInfoId: String,
    ): ByteArray

    fun hentJournalposter(tilbakekreving: Tilbakekreving): List<Journalpost>
}
