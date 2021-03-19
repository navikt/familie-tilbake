package no.nav.familie.tilbake.beregning

import no.nav.familie.tilbake.beregning.modell.BeregningResultatPeriode
import no.nav.familie.tilbake.beregning.modell.FordeltKravgrunnlagBeløp
import no.nav.familie.tilbake.beregning.modell.GrunnlagPeriodeMedSkattProsent
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.common.isZero
import no.nav.familie.tilbake.domain.tbd.Aktsomhet
import no.nav.familie.tilbake.domain.tbd.AnnenVurdering
import no.nav.familie.tilbake.domain.tbd.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.domain.tbd.VilkårsvurderingGodTro
import no.nav.familie.tilbake.domain.tbd.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.domain.tbd.Vurdering
import java.math.BigDecimal
import java.math.RoundingMode

internal object TilbakekrevingBeregnerVilkår {

    private val HUNDRE_PROSENT: BigDecimal = BigDecimal.valueOf(100)
    private val RENTESATS: BigDecimal = BigDecimal.valueOf(10)
    private val RENTEFAKTOR: BigDecimal = RENTESATS.divide(HUNDRE_PROSENT, 2, RoundingMode.UNNECESSARY)

    fun beregn(vilkårVurdering: Vilkårsvurderingsperiode,
               delresultat: FordeltKravgrunnlagBeløp,
               perioderMedSkattProsent: List<GrunnlagPeriodeMedSkattProsent>,
               beregnRenter: Boolean): BeregningResultatPeriode {
        val periode: Periode = vilkårVurdering.periode
        val vurdering: Vurdering = finnVurdering(vilkårVurdering)
        val renter = beregnRenter && finnRenter(vilkårVurdering)
        val andel: BigDecimal? = finnAndelAvBeløp(vilkårVurdering)
        val manueltBeløp: BigDecimal? = finnManueltSattBeløp(vilkårVurdering)
        val ignoreresPgaLavtBeløp = false == vilkårVurdering.aktsomhet?.tilbakekrevSmåbeløp
        val beløpUtenRenter: BigDecimal =
                if (ignoreresPgaLavtBeløp) BigDecimal.ZERO else finnBeløpUtenRenter(delresultat.feilutbetaltBeløp,
                                                                                    andel,
                                                                                    manueltBeløp)
        val rentebeløp: BigDecimal = beregnRentebeløp(beløpUtenRenter, renter)
        val tilbakekrevingBeløp: BigDecimal = beløpUtenRenter.add(rentebeløp)
        val skattBeløp: BigDecimal =
                beregnSkattBeløp(periode,
                                 beløpUtenRenter,
                                 perioderMedSkattProsent)
        val nettoBeløp: BigDecimal = tilbakekrevingBeløp.subtract(skattBeløp)
        return BeregningResultatPeriode(periode = periode,
                                        vurdering = vurdering,
                                        renterProsent = if (renter) RENTESATS else null,
                                        feilutbetaltBeløp = delresultat.feilutbetaltBeløp,
                                        riktigYtelseBeløp = delresultat.riktigYtelseBeløp,
                                        utbetaltYtelseBeløp = delresultat.utbetaltYtelseBeløp,
                                        andelAvBeløp = andel,
                                        manueltSattTilbakekrevingsbeløp = manueltBeløp,
                                        tilbakekrevingBeløpUtenRenter = beløpUtenRenter,
                                        renteBeløp = rentebeløp,
                                        tilbakekrevingBeløpEtterSkatt = nettoBeløp,
                                        skattBeløp = skattBeløp,
                                        tilbakekrevingBeløp = tilbakekrevingBeløp)
    }

    private fun beregnRentebeløp(beløp: BigDecimal, renter: Boolean): BigDecimal {
        return if (renter) beløp.multiply(RENTEFAKTOR).setScale(0, RoundingMode.HALF_UP) else BigDecimal.ZERO
    }

    private fun beregnSkattBeløp(periode: Periode,
                                 bruttoTilbakekrevesBeløp: BigDecimal,
                                 perioderMedSkattProsent: List<GrunnlagPeriodeMedSkattProsent>): BigDecimal {
        val totalKgTilbakekrevesBeløp: BigDecimal = perioderMedSkattProsent.sumOf { it.tilbakekrevesBeløp }
        val andel: BigDecimal = if (totalKgTilbakekrevesBeløp.isZero()) {
            BigDecimal.ZERO
        } else {
            bruttoTilbakekrevesBeløp.divide(totalKgTilbakekrevesBeløp, 4, RoundingMode.HALF_UP)
        }
        var skattBeløp: BigDecimal = BigDecimal.ZERO
        for (grunnlagPeriodeMedSkattProsent in perioderMedSkattProsent) {
            if (periode.overlapper(grunnlagPeriodeMedSkattProsent.periode)) {
                val delTilbakekrevesBeløp: BigDecimal = grunnlagPeriodeMedSkattProsent.tilbakekrevesBeløp.multiply(andel)
                skattBeløp = skattBeløp.add(delTilbakekrevesBeløp.multiply(grunnlagPeriodeMedSkattProsent.skattProsent)
                                                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                val statement = ""
            }
        }
        return skattBeløp.setScale(0, RoundingMode.DOWN)
    }

    private fun finnBeløpUtenRenter(kravgrunnlagBeløp: BigDecimal, andel: BigDecimal?, manueltBeløp: BigDecimal?): BigDecimal {
        if (manueltBeløp != null) {
            return manueltBeløp
        }
        if (andel != null) {
            return kravgrunnlagBeløp.multiply(andel).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
        }
        throw IllegalArgumentException("Utvikler-feil: Forventer at utledet andel eller manuelt beløp er satt begge manglet")
    }

    private fun finnRenter(vurdering: Vilkårsvurderingsperiode): Boolean {
        val aktsomhet: VilkårsvurderingAktsomhet? = vurdering.aktsomhet
        if (aktsomhet != null) {
            val erForsett: Boolean = Aktsomhet.FORSETT.equals(aktsomhet.aktsomhet)
            return erForsett && (aktsomhet.ileggRenter == null || aktsomhet.ileggRenter) ||
                   aktsomhet.ileggRenter != null && aktsomhet.ileggRenter
        }
        return false
    }

    private fun finnAndelAvBeløp(vurdering: Vilkårsvurderingsperiode): BigDecimal? {
        val aktsomhet: VilkårsvurderingAktsomhet? = vurdering.aktsomhet
        val godTro: VilkårsvurderingGodTro? = vurdering.godTro
        if (aktsomhet != null) {
            return finnAndelForAktsomhet(aktsomhet)
        } else if (godTro != null && !godTro.beløpErIBehold) {
            return BigDecimal.ZERO
        }
        return null
    }

    private fun finnAndelForAktsomhet(aktsomhet: VilkårsvurderingAktsomhet): BigDecimal? {
        return if (Aktsomhet.FORSETT == aktsomhet.aktsomhet || !aktsomhet.særligeGrunnerTilReduksjon) {
            HUNDRE_PROSENT
        } else aktsomhet.andelTilbakekreves
    }

    private fun finnManueltSattBeløp(vurdering: Vilkårsvurderingsperiode): BigDecimal? {
        val aktsomhet: VilkårsvurderingAktsomhet? = vurdering.aktsomhet
        val godTro: VilkårsvurderingGodTro? = vurdering.godTro
        if (aktsomhet != null) {
            return aktsomhet.manueltSattBeløp
        } else if (godTro != null) {
            return godTro.beløpTilbakekreves
        }
        throw IllegalArgumentException("VVurdering skal peke til GodTro-entiet eller Aktsomhet-entitet")
    }

    private fun finnVurdering(vurdering: Vilkårsvurderingsperiode): Vurdering {
        if (vurdering.aktsomhet != null) {
            return vurdering.aktsomhet.aktsomhet
        }
        if (vurdering.godTro != null) {
            return AnnenVurdering.GOD_TRO
        }
        throw IllegalArgumentException("VVurdering skal peke til GodTro-entiet eller Aktsomhet-entitet")
    }
}