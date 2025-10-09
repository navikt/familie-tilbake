package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Faktasteg
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import java.util.UUID

data class FaktastegEntity(
    val id: UUID,
    val behandlingRef: UUID,
    val perioder: List<FaktaPeriodeEntity>,
    val årsakTilFeilutbetaling: String,
    val uttalelse: Uttalelse,
    val vurderingAvBrukersUttalelse: String?,
) {
    fun fraEntity(
        brevHistorikk: BrevHistorikk,
    ): Faktasteg = Faktasteg(
        id = id,
        brevHistorikk = brevHistorikk,
        vurdering = Faktasteg.Vurdering(
            perioder = perioder.map {
                Faktasteg.FaktaPeriode(
                    id = it.id,
                    periode = it.periode.fraEntity(),
                    rettsligGrunnlag = it.rettsligGrunnlag,
                    rettsligGrunnlagUnderkategori = it.rettsligGrunnlagUnderkategori,
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
        val id: UUID,
        val faktavurderingRef: UUID,
        val periode: DatoperiodeEntity,
        val rettsligGrunnlag: Hendelsestype,
        val rettsligGrunnlagUnderkategori: Hendelsesundertype,
    )
}
