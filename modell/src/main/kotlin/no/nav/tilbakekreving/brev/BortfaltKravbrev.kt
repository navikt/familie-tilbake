package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.entities.BortfaltKravbrevEntity
import no.nav.tilbakekreving.entities.BrevEntity
import no.nav.tilbakekreving.entities.Brevtype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class BortfaltKravbrev(
    override val id: UUID,
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
            brevtype = Brevtype.BORTFALT_KRAVBREV,
            varselbrevEntity = null,
            vedtaksbrevEntity = null,
            bortfaltKravbrevEntity = BortfaltKravbrevEntity(
                id = id,
                journalpostId = journalpostId,
                dokumentInfoId = dokumentInfoId,
                sendtTid = sendtTid,
            ),
            opprettet = opprettet,
        )
    }

    companion object {
        fun opprett(klokke: Klokke): BortfaltKravbrev {
            return BortfaltKravbrev(
                id = UUID.randomUUID(),
                journalpostId = null,
                dokumentInfoId = null,
                sendtTid = klokke.dagensDato(),
                opprettet = klokke.nå(),
            )
        }
    }
}
