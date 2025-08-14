package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.saksbehandling.Faktasteg
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import java.time.LocalDateTime
import java.util.UUID

data class FaktastegEntity(
    val tilbakekrevingOpprettet: LocalDateTime,
    val opprettelsesvalg: Opprettelsesvalg,
    val perioder: List<FaktaPeriodeEntity>,
    val årsakTilFeilutbetaling: String,
    val uttalelse: Uttalelse,
    val vurderingAvBrukersUttalelse: String?,
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
        vurdering = Faktasteg.Vurdering(
            perioder = perioder.map {
                Faktasteg.FaktaPeriode(
                    periode = it.periode.fraEntity(),
                    hendelsestype = it.hendelsestype,
                    hendelsesundertype = it.hendelsesundertype,
                )
            },
            årsakTilFeilutbetaling = årsakTilFeilutbetaling,
            uttalelse = when (uttalelse) {
                Uttalelse.Ja -> Faktasteg.Uttalelse.Ja(vurderingAvBrukersUttalelse!!)
                Uttalelse.Nei -> Faktasteg.Uttalelse.Nei
                Uttalelse.IkkeAktuelt -> Faktasteg.Uttalelse.IkkeAktuelt
                Uttalelse.IkkeVurdert -> Faktasteg.Uttalelse.IkkeVurdert
            },
        ),
    )

    enum class Uttalelse {
        Ja,
        Nei,
        IkkeAktuelt,
        IkkeVurdert,
    }

    class FaktaPeriodeEntity(
        val periode: DatoperiodeEntity,
        val hendelsestype: Hendelsestype,
        val hendelsesundertype: Hendelsesundertype,
    )
}
