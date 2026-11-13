package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.brev.Vedtaksbrev
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class VedtaksbrevEntity(
    val id: UUID,
    val journalpostId: String?,
    val dokumentInfoId: String?,
    val sendtTid: LocalDate,
    val vedtaksbrevtype: Vedtaksbrev.Vedtaksbrevtype,
) {
    fun fraEntity(id: UUID, opprettet: LocalDateTime) = Vedtaksbrev(
        id = id,
        journalpostId = journalpostId,
        dokumentInfoId = dokumentInfoId,
        sendtTid = sendtTid,
        opprettet = opprettet,
        vedtaksbrevType = vedtaksbrevtype,
    )
}
