package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.brev.Informasjonsbrev
import no.nav.tilbakekreving.brev.Informasjonsbrevtype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class InformasjonsbrevEntity(
    val id: UUID,
    val type: Informasjonsbrevtype,
    val journalpostId: String?,
    val dokumentInfoId: String?,
    val sendtTid: LocalDate,
) {
    fun fraEntity(id: UUID, opprettet: LocalDateTime) = Informasjonsbrev(
        id = id,
        type = type,
        journalpostId = journalpostId,
        dokumentInfoId = dokumentInfoId,
        sendtTid = sendtTid,
        opprettet = opprettet,
    )
}
