package no.nav.tilbakekreving.beregning.delperiode

import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.beregning.delperiode.Delperiode.Beløp.Companion.forKlassekode
import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
import java.math.BigDecimal
import java.math.RoundingMode

class Foreldet(
    override val periode: Datoperiode,
    override val delperioder: List<ForeldetPeriode>,
) : Vurderingsperiode<Foreldet.ForeldetPeriode> {
    override fun beregningsresultat(): Beregningsresultatsperiode {
        return Beregningsresultatsperiode(
            periode = periode,
            feilutbetaltBeløp = delperioder.sumOf { it.feilutbetaltBeløp() },
            riktigYtelsesbeløp = delperioder.sumOf { it.summer(ForeldetBeløp::riktigYtelsesbeløp) }.setScale(0, RoundingMode.HALF_UP),
            utbetaltYtelsesbeløp = delperioder.sumOf { it.summer(ForeldetBeløp::utbetaltYtelsesbeløp) }.setScale(0, RoundingMode.HALF_UP),
            tilbakekrevingsbeløp = delperioder.sumOf { it.tilbakekrevesBruttoMedRenter() },
            tilbakekrevingsbeløpUtenRenter = delperioder.sumOf { it.summer(ForeldetBeløp::tilbakekrevesBrutto) },
            rentebeløp = delperioder.sumOf { it.renter() },
            andelAvBeløp = BigDecimal.ZERO,
            vurdering = AnnenVurdering.FORELDET,
            skattebeløp = delperioder.sumOf { it.summer(ForeldetBeløp::skatt) },
            tilbakekrevingsbeløpEtterSkatt = BigDecimal.ZERO,
        )
    }

    class ForeldetPeriode(
        override val vurdertPeriode: Datoperiode,
        override val periode: Datoperiode,
        private val beløp: List<ForeldetBeløp>,
        val kravgrunnlagPeriode: KravgrunnlagPeriodeAdapter,
    ) : Delperiode {
        override fun renter(): BigDecimal = BigDecimal.ZERO

        override fun tilbakekrevesBruttoMedRenter(): BigDecimal = BigDecimal.ZERO

        override fun beløpForKlassekode(klassekode: String): Delperiode.Beløp = beløp.forKlassekode(klassekode)

        fun summer(hentBeløp: ForeldetBeløp.() -> BigDecimal) = beløp.sumOf { it.hentBeløp() }

        override fun beløp(): List<Delperiode.Beløp> {
            return beløp
        }

        override fun feilutbetaltBeløp(): BigDecimal {
            return kravgrunnlagPeriode.feilutbetaltYtelsesbeløp().setScale(0, RoundingMode.HALF_UP)
        }
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
                        vurdertPeriode,
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
