package no.nav.tilbakekreving.beregning.delperiode

import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
import java.math.BigDecimal
import java.math.RoundingMode

class Foreldet(
    override val periode: Datoperiode,
    override val delperioder: List<ForeldetPeriode>,
) : Vurderingsperiode<ForeldetBeløp, Foreldet.ForeldetPeriode> {
    override fun beregningsresultat(): Beregningsresultatsperiode {
        return Beregningsresultatsperiode(
            periode = periode,
            feilutbetaltBeløp = delperioder.sumOf { it.feilutbetaltBeløp() },
            riktigYtelsesbeløp = delperioder.sumOf { it.summer(Delperiode.Beløp::riktigYtelsesbeløp) }.setScale(0, RoundingMode.HALF_UP),
            utbetaltYtelsesbeløp = delperioder.sumOf { it.summer(Delperiode.Beløp::utbetaltYtelsesbeløp) }.setScale(0, RoundingMode.HALF_UP),
            tilbakekrevingsbeløp = delperioder.sumOf { it.tilbakekrevesBruttoMedRenter() },
            tilbakekrevingsbeløpUtenRenter = delperioder.sumOf { it.summer(Delperiode.Beløp::tilbakekrevesBrutto) },
            rentebeløp = delperioder.sumOf { it.renter() },
            andelAvBeløp = BigDecimal.ZERO,
            vurdering = AnnenVurdering.FORELDET,
            skattebeløp = delperioder.sumOf { it.summer(Delperiode.Beløp::skatt) },
            tilbakekrevingsbeløpEtterSkatt = BigDecimal.ZERO,
        )
    }

    class ForeldetPeriode(
        override val periode: Datoperiode,
        beløp: List<ForeldetBeløp>,
        kravgrunnlagPeriode: KravgrunnlagPeriodeAdapter,
    ) : Delperiode<ForeldetBeløp>(kravgrunnlagPeriode, beløp) {
        override fun renter(): BigDecimal = BigDecimal.ZERO

        override fun tilbakekrevesBruttoMedRenter(): BigDecimal = BigDecimal.ZERO
    }

    companion object {
        fun opprett(
            vurdertPeriode: Datoperiode,
            kravgrunnlagPerioder: List<KravgrunnlagPeriodeAdapter>,
        ): Foreldet {
            return Foreldet(
                vurdertPeriode,
                kravgrunnlagPerioder.map { kravgrunnlagPeriode ->
                    val delperiode = requireNotNull(kravgrunnlagPeriode.periode().snitt(vurdertPeriode)) {
                        "Finner ingen kravgrunnlagsperiode som er dekket av foreldelsesperioden $vurdertPeriode, kravgrunnlagsperiode=${kravgrunnlagPeriode.periode()}"
                    }
                    ForeldetPeriode(
                        delperiode,
                        kravgrunnlagPeriode.beløpTilbakekreves().map {
                            ForeldetBeløp(it.klassekode(), delperiode, it)
                        },
                        kravgrunnlagPeriode,
                    )
                },
            )
        }
    }
}
