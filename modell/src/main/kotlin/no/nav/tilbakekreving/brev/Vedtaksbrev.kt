package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.entities.BrevEntity
import no.nav.tilbakekreving.entities.Brevtype
import no.nav.tilbakekreving.entities.VedtaksbrevEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Vedtaksbrev(
    override val id: UUID,
    override var journalpostId: String?,
    override var dokumentInfoId: String?,
    override var sendtTid: LocalDate,
    override val opprettet: LocalDateTime,
    val vedtaksbrevType: Vedtaksbrevtype,
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
                journalpostId = journalpostId,
                dokumentInfoId = dokumentInfoId,
                sendtTid = sendtTid,
                vedtaksbrevtype = vedtaksbrevType,
            ),
            informasjonsbrevEntity = null,
            opprettet = opprettet,
        )
    }

    companion object {
        fun opprett(klokke: Klokke, vedtaksbrevtype: Vedtaksbrevtype): Vedtaksbrev {
            return Vedtaksbrev(
                id = UUID.randomUUID(),
                journalpostId = null,
                dokumentInfoId = null,
                sendtTid = klokke.dagensDato(),
                opprettet = klokke.nå(),
                vedtaksbrevType = vedtaksbrevtype,
            )
        }
    }

    enum class Vedtaksbrevtype {
        FØRSTEGANG,
        REVURDERING,
    }
}
