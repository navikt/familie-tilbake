package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.brev.Vedtaksbrev
import java.time.LocalDate
import java.util.UUID

data class VedtaksbrevEntity(
    val id: UUID,
    val brevRef: UUID,
    val journalpostId: String?,
    val sendtTid: LocalDate,
) {
    fun fraEntity(id: UUID) = Vedtaksbrev(
        id = id,
        journalpostId = journalpostId,
        sendtTid = sendtTid,
    )
}
