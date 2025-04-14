package no.nav.tilbakekreving.beregning

import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagAdapter
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.beregning.modell.FordeltKravgrunnlagsbeløp
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.math.RoundingMode

object KravgrunnlagsberegningUtil {
    fun fordelKravgrunnlagBeløpPåPerioder(
        kravgrunnlag: KravgrunnlagAdapter,
        vurderingsperioder: List<Datoperiode>,
    ): Map<Datoperiode, FordeltKravgrunnlagsbeløp> =
        vurderingsperioder.associateWith {
            FordeltKravgrunnlagsbeløp(
                beregnBeløp(kravgrunnlag, it, KravgrunnlagPeriodeAdapter::feilutbetaltYtelsesbeløp),
                beregnBeløp(kravgrunnlag, it, KravgrunnlagPeriodeAdapter::utbetaltYtelsesbeløp),
                beregnBeløp(kravgrunnlag, it, KravgrunnlagPeriodeAdapter::riktigYteslesbeløp),
            )
        }

    fun summerKravgrunnlagBeløpForPerioder(kravgrunnlag: KravgrunnlagAdapter): Map<Datoperiode, FordeltKravgrunnlagsbeløp> =
        kravgrunnlag.perioder().associate {
            it.periode() to
                FordeltKravgrunnlagsbeløp(
                    it.feilutbetaltYtelsesbeløp(),
                    it.utbetaltYtelsesbeløp(),
                    it.riktigYteslesbeløp(),
                )
        }

    fun beregnFeilutbetaltBeløp(
        kravgrunnlag: KravgrunnlagAdapter,
        vurderingsperiode: Datoperiode,
    ): BigDecimal = beregnBeløp(kravgrunnlag, vurderingsperiode, KravgrunnlagPeriodeAdapter::feilutbetaltYtelsesbeløp)

    private fun beregnBeløp(
        kravgrunnlag: KravgrunnlagAdapter,
        vurderingsperiode: Datoperiode,
        beløpsummerer: KravgrunnlagPeriodeAdapter.() -> BigDecimal,
    ): BigDecimal {
        return kravgrunnlag.perioder()
            .sortedBy { it.periode().fom }
            .filter { it.beløpsummerer().isNotZero() }
            .sumOf {
                val beløp = it.beløpsummerer()
                val beløpPerMåned = BeløpsberegningUtil.beregnBeløpPerMåned(beløp, it.periode())
                BeløpsberegningUtil.beregnBeløp(vurderingsperiode, it.periode(), beløpPerMåned)
            }
            .setScale(0, RoundingMode.HALF_UP)
    }
}
