package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.saksbehandling.Faktasteg
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.time.LocalDateTime
import java.util.UUID

data class FaktastegEntity(
    val eksternFagsakBehandlingRef: UUID,
    val kravgrunnlagRef: UUID,
    val brevHistorikk: BrevHistorikkEntity,
    val tilbakekrevingOpprettet: LocalDateTime,
    val opprettelsesvalg: String,
) {
    fun fraEntity(
        eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
        kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
    ): Faktasteg = Faktasteg(
        eksternFagsakBehandling = eksternFagsakBehandling,
        kravgrunnlag = kravgrunnlag,
        brevHistorikk = brevHistorikk.fraEntity(),
        tilbakekrevingOpprettet = tilbakekrevingOpprettet,
        opprettelsesvalg = Opprettelsesvalg.valueOf(opprettelsesvalg),
    )
}
