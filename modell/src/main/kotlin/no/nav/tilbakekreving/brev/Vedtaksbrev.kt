package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.entities.BrevEntity
import no.nav.tilbakekreving.entities.Brevtype
import no.nav.tilbakekreving.entities.VedtaksbrevEntity
import java.time.LocalDate
import java.util.UUID

data class Vedtaksbrev(
    override val id: UUID,
    override var journalpostId: String?,
    override var dokumentInfoId: String?,
    override var sendtTid: LocalDate,
) : Brev {
    override fun brevSendt(journalpostId: String, dokumentInfoId: String) {
        this.journalpostId = journalpostId
        this.dokumentInfoId = dokumentInfoId
    }

    override fun tilEntity(tilbakekrevingId: String): BrevEntity {
        return BrevEntity(
            id = id,
            tilbakekrevingRef = tilbakekrevingId,
            brevtype = Brevtype.VEDTAKSBREV,
            varselbrevEntity = null,
            vedtaksbrevEntity = VedtaksbrevEntity(
                id = id,
                brevRef = id,
                journalpostId = journalpostId,
                dokumentInfoId = dokumentInfoId,
                sendtTid = sendtTid,
            ),
        )
    }

    companion object {
        fun opprett(klokke: Klokke): Vedtaksbrev {
            return Vedtaksbrev(
                id = UUID.randomUUID(),
                journalpostId = null,
                dokumentInfoId = null,
                sendtTid = klokke.dagensDato(),
            )
        }
    }
}
