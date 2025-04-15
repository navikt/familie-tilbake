package no.nav.tilbakekreving.beregning

import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurderingAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurdertPeriodeAdapter
import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode
import no.nav.tilbakekreving.beregning.modell.FordeltKravgrunnlagsbeløp
import no.nav.tilbakekreving.beregning.modell.GrunnlagsperiodeMedSkatteprosent
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode

class VilkårsvurderingBeregning(
    private val kravgrunnlag: KravgrunnlagAdapter,
    private val vilkårsvurdering: VilkårsvurderingAdapter,
    val kravbeløpPerPeriode: Map<Datoperiode, FordeltKravgrunnlagsbeløp>,
    val beregnRenter: Boolean,
) {
    internal fun beregn(): Collection<Beregningsresultatsperiode> = vilkårsvurdering
        .perioder()
        .map { beregnPeriode(it, kravbeløpPerPeriode) }

    private fun beregnPeriode(
        vurdering: VilkårsvurdertPeriodeAdapter,
        kravbeløpPerPeriode: Map<Datoperiode, FordeltKravgrunnlagsbeløp>,
    ): Beregningsresultatsperiode {
        val delresultat = kravbeløpPerPeriode[vurdering.periode()]
            ?: error("Periode i finnes ikke i map kravbeløpPerPeriode")

        val perioderMedSkattProsent = lagGrunnlagPeriodeMedSkattProsent(vurdering.periode())

        return TilbakekrevingsberegningVilkår.beregn(
            vurdering,
            delresultat,
            perioderMedSkattProsent,
            beregnRenter,
        )
    }

    private fun lagGrunnlagPeriodeMedSkattProsent(
        vurderingsperiode: Datoperiode,
    ): List<GrunnlagsperiodeMedSkatteprosent> =
        kravgrunnlag.perioder()
            .sortedBy { it.periode().fom }
            .map {
                it.beløpTilbakekreves().map { kgBeløp ->
                    GrunnlagsperiodeMedSkatteprosent(
                        periode = it.periode(),
                        tilbakekrevingsbeløp = BeløpsberegningUtil.beregnBeløpForPeriode(
                            kgBeløp.beløp(),
                            vurderingsperiode,
                            it.periode(),
                        ),
                        skatteprosent = kgBeløp.skatteprosent(),
                    )
                }
            }.flatten()
}
