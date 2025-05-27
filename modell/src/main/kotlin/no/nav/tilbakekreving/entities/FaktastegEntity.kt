package no.nav.tilbakekreving.entities

import java.time.LocalDateTime
import java.util.UUID

data class FaktastegEntity(
    val eksternFagsakBehandlingRef: UUID,
    val kravgrunnlagRef: UUID,
    val brevHistorikk: BrevHistorikkEntity,
    val tilbakekrevingOpprettet: LocalDateTime,
    val opprettelsesvalg: String,
)
