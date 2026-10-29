package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.entities.BrevEntity
import no.nav.tilbakekreving.entities.Brevtype
import no.nav.tilbakekreving.entities.InformasjonsbrevEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Informasjonsbrev(
    override val id: UUID,
    val type: Informasjonsbrevtype,
    override var journalpostId: String?,
    override var dokumentInfoId: String?,
    override var sendtTid: LocalDate,
    override val opprettet: LocalDateTime,
) : Brev {
    override fun brevSendt(journalpostId: String, dokumentInfoId: String) {
        this.journalpostId = journalpostId
        this.dokumentInfoId = dokumentInfoId
    }

    override fun tilEntity(tilbakekrevingId: String): BrevEntity {
        return BrevEntity(
            id = id,
            tilbakekrevingRef = tilbakekrevingId,
            brevtype = Brevtype.INFORMASJONSBREV,
            varselbrevEntity = null,
            vedtaksbrevEntity = null,
            informasjonsbrevEntity = InformasjonsbrevEntity(
                id = id,
                type = type,
                journalpostId = journalpostId,
                dokumentInfoId = dokumentInfoId,
                sendtTid = sendtTid,
            ),
            opprettet = opprettet,
        )
    }

    companion object {
        fun opprett(type: Informasjonsbrevtype, klokke: Klokke): Informasjonsbrev {
            return Informasjonsbrev(
                id = UUID.randomUUID(),
                type = type,
                journalpostId = null,
                dokumentInfoId = null,
                sendtTid = klokke.dagensDato(),
                opprettet = klokke.nå(),
            )
        }
    }
}
