package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behov.BehovObservatør
import java.time.LocalDateTime
import java.util.UUID

data class TilbakekrevingEntity(
    val id: UUID = UUID.randomUUID(),
    val nåværendeTilstand: String,
    val eksternFagsak: EksternFagsakEntity,
    val behandlingHistorikk: BehandlingHistorikkEntity,
    val kravgrunnlagHistorikk: KravgrunnlagHistorikkEntity,
    val brevHistorikk: BrevHistorikkEntity,
    val opprettet: LocalDateTime,
    val opprettelsesvalg: String,
    var bruker: BrukerEntity? = null,
){
    fun fraEntity(
        behovObservatør: BehovObservatør,
    ): Tilbakekreving {
        return Tilbakekreving(
            id = id,
            eksternFagsak = eksternFagsak.fraEntity(behovObservatør),
            behandlingHistorikk = behandlingHistorikk.fraEntity(eksternFagsak, kravgrunnlagHistorikk),
            kravgrunnlagHistorikk =  kravgrunnlagHistorikk.fraEntity(),
            brevHistorikk = brevHistorikk.fraEntity(),
            opprettet = opprettet,
            opprettelsesvalg = Opprettelsesvalg.valueOf(opprettelsesvalg),
            behovObservatør = behovObservatør,
        )
    }
}
