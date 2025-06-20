package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.saksbehandling.Faktasteg
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.time.LocalDateTime
import java.util.UUID

data class FaktastegEntity(
    val tilbakekrevingOpprettet: LocalDateTime,
    val opprettelsesvalg: Opprettelsesvalg,
) {
    fun fraEntity(
        eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
        kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
        brevHistorikk: BrevHistorikk,
    ): Faktasteg = Faktasteg(
        eksternFagsakBehandling = eksternFagsakBehandling,
        kravgrunnlag = kravgrunnlag,
        brevHistorikk = brevHistorikk,
        tilbakekrevingOpprettet = tilbakekrevingOpprettet,
        opprettelsesvalg = opprettelsesvalg,
    )
}
