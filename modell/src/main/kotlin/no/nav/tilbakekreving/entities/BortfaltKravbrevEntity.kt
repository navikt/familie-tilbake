package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.brev.BortfaltKravbrev
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class BortfaltKravbrevEntity(
    val id: UUID,
    val journalpostId: String?,
    val dokumentInfoId: String?,
    val sendtTid: LocalDate,
) {
    fun fraEntity(id: UUID, opprettet: LocalDateTime) = BortfaltKravbrev(
        id = id,
        journalpostId = journalpostId,
        dokumentInfoId = dokumentInfoId,
        sendtTid = sendtTid,
        opprettet = opprettet,
    )
}
