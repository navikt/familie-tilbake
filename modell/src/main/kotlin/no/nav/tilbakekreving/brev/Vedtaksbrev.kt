package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.entities.BrevEntity
import no.nav.tilbakekreving.entities.Brevtype
import no.nav.tilbakekreving.entities.VedtaksbrevEntity
import java.time.LocalDate
import java.util.UUID

data class Vedtaksbrev(
    override val id: UUID,
    override var journalpostId: String?,
    override var sendtTid: LocalDate,
) : Brev {
    override fun brevSendt(journalpostId: String) {
        this.journalpostId = journalpostId
    }

    override fun tilEntity(tilbakekrevingId: String): BrevEntity {
        return BrevEntity(
            id = id,
            tilbakekrevingRef = tilbakekrevingId,
            brevtype = Brevtype.VEDTAKSBREV,
            varselbrevEntity = null,
            vedtaksbrevEntity = VedtaksbrevEntity(
                id = UUID.randomUUID(),
                brevRef = id,
                journalpostId = journalpostId,
                sendtTid = sendtTid,
            ),
        )
    }

    companion object {
        fun opprett(): Vedtaksbrev {
            return Vedtaksbrev(
                id = UUID.randomUUID(),
                journalpostId = null,
                sendtTid = LocalDate.now(),
            )
        }
    }
}
