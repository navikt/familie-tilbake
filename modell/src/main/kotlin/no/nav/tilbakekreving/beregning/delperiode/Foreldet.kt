package no.nav.tilbakekreving.beregning.delperiode

import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
import java.math.BigDecimal
import java.math.RoundingMode

class Foreldet(
    override val periode: Datoperiode,
    override val vurdertPeriode: Datoperiode,
    override val andel: Andel,
) : Delperiode {
    override fun tilbakekrevesBruttoMedRenter(): BigDecimal = BigDecimal.ZERO

    override fun tilbakekrevesBrutto(): BigDecimal = BigDecimal.ZERO

    override fun skatt(): BigDecimal = BigDecimal.ZERO

    override fun renter(): BigDecimal = BigDecimal.ZERO

    override fun beregningsresultat(): Beregningsresultatsperiode {
        return Beregningsresultatsperiode(
            periode = periode,
            feilutbetaltBeløp = andel.feilutbetaltBeløp().setScale(0, RoundingMode.HALF_UP),
            riktigYtelsesbeløp = andel.riktigYtelsesbeløp().setScale(0, RoundingMode.HALF_UP),
            utbetaltYtelsesbeløp = andel.utbetaltYtelsesbeløp().setScale(0, RoundingMode.HALF_UP),
            tilbakekrevingsbeløp = tilbakekrevesBruttoMedRenter(),
            tilbakekrevingsbeløpUtenRenter = tilbakekrevesBrutto(),
            rentebeløp = renter(),
            andelAvBeløp = BigDecimal.ZERO,
            vurdering = AnnenVurdering.FORELDET,
            skattebeløp = skatt(),
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
            return Foreldet(delperiode, vurdertPeriode, Andel(kravgrunnlagPeriode, delperiode))
        }
    }
}
