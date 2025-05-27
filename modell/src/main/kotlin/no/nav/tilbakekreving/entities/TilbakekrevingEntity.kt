package no.nav.tilbakekreving.entities

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
)
