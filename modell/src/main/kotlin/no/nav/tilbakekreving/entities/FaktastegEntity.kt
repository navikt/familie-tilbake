package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Faktasteg
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import java.time.LocalDate
import java.util.UUID

data class FaktastegEntity(
    val id: UUID,
    val behandlingRef: UUID,
    val perioder: List<FaktaPeriodeEntity>,
    val årsakTilFeilutbetaling: String,
    val uttalelse: Uttalelse,
    val vurderingAvBrukersUttalelse: String?,
    val oppdaget: OppdagetEntity?,
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
            oppdaget = oppdaget?.fraEntity() ?: Faktasteg.Vurdering.Oppdaget.IkkeVurdert,
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

    enum class OppdagetAv {
        Nav,
        Bruker,
    }

    class OppdagetEntity(
        val id: UUID,
        val faktavurderingRef: UUID,
        val av: OppdagetAv,
        val dato: LocalDate,
        val beskrivelse: String,
    ) {
        internal fun fraEntity(): Faktasteg.Vurdering.Oppdaget.Vurdering {
            return Faktasteg.Vurdering.Oppdaget.Vurdering(
                id = id,
                dato = dato,
                beskrivelse = beskrivelse,
                av = when (av) {
                    OppdagetAv.Nav -> Faktasteg.Vurdering.Oppdaget.Av.Nav
                    OppdagetAv.Bruker -> Faktasteg.Vurdering.Oppdaget.Av.Bruker
                },
            )
        }
    }
}
