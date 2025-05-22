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
    override val vurdertPeriode: Datoperiode,
    val beløp: List<Delperiode.Beløp>,
    val kravgrunnlagPeriode: KravgrunnlagPeriodeAdapter,
) : Delperiode {
    override fun renter(): BigDecimal = BigDecimal.ZERO

    override fun tilbakekrevesBruttoMedRenter(): BigDecimal = BigDecimal.ZERO

    override fun beløpForKlassekode(klassekode: String): Delperiode.Beløp = beløp.forKlassekode(klassekode)

    override fun beløp(): List<Delperiode.Beløp> {
        return beløp
    }

    override fun feilutbetaltBeløp(): BigDecimal {
        return kravgrunnlagPeriode.feilutbetaltYtelsesbeløp().setScale(0, RoundingMode.HALF_UP)
    }

    override fun beregningsresultat(): Beregningsresultatsperiode {
        return Beregningsresultatsperiode(
            periode = periode,
            feilutbetaltBeløp = feilutbetaltBeløp(),
            riktigYtelsesbeløp = beløp.sumOf { it.riktigYtelsesbeløp() }.setScale(0, RoundingMode.HALF_UP),
            utbetaltYtelsesbeløp = beløp.sumOf { it.utbetaltYtelsesbeløp() }.setScale(0, RoundingMode.HALF_UP),
            tilbakekrevingsbeløp = tilbakekrevesBruttoMedRenter(),
            tilbakekrevingsbeløpUtenRenter = beløp().sumOf { it.tilbakekrevesBrutto() },
            rentebeløp = renter(),
            andelAvBeløp = BigDecimal.ZERO,
            vurdering = AnnenVurdering.FORELDET,
            skattebeløp = beløp().sumOf { it.skatt() },
            tilbakekrevingsbeløpEtterSkatt = BigDecimal.ZERO,
        )
    }

    companion object {
        fun opprett(
            vurdertPeriode: Datoperiode,
            kravgrunnlagPeriode: KravgrunnlagPeriodeAdapter,
        ): Foreldet {
            val delperiode = requireNotNull(kravgrunnlagPeriode.periode().snitt(vurdertPeriode)) {
                "Finner ingen kravgrunnlagsperiode som er dekket av foreldelsesperioden $vurdertPeriode, kravgrunnlagsperiode=${kravgrunnlagPeriode.periode()}"
            }
            return Foreldet(delperiode, vurdertPeriode, kravgrunnlagPeriode.beløpTilbakekreves().map { ForeldetBeløp(it.klassekode(), delperiode, it) }, kravgrunnlagPeriode)
        }
    }
}
