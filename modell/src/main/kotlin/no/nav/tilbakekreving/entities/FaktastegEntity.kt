package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.saksbehandling.Faktasteg
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class FaktastegEntity(
    val eksternFagsakBehandlingRef: String,
    val kravgrunnlagRef: String,
    val brevHistorikk: BrevHistorikkEntity,
    val tilbakekrevingOpprettet: String,
    val opprettelsesvalg: String,
) {
    fun fraEntity(
        eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
        kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
    ): Faktasteg = Faktasteg(
        eksternFagsakBehandling = eksternFagsakBehandling,
        kravgrunnlag = kravgrunnlag,
        brevHistorikk = brevHistorikk.fraEntity(),
        tilbakekrevingOpprettet = LocalDateTime.parse(tilbakekrevingOpprettet),
        opprettelsesvalg = Opprettelsesvalg.valueOf(opprettelsesvalg),
    )
}
