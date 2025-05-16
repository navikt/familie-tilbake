package no.nav.tilbakekreving.beregning.modell

import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering
import java.math.BigDecimal

data class Beregningsresultatsperiode(
    val periode: Datoperiode,
    val vurdering: Vurdering? = null,
    val feilutbetaltBeløp: BigDecimal,
    val andelAvBeløp: BigDecimal? = null,
    val renteprosent: BigDecimal? = null,
    val manueltSattTilbakekrevingsbeløp: BigDecimal? = null,
    val tilbakekrevingsbeløpUtenRenter: BigDecimal,
    val rentebeløp: BigDecimal,
    val tilbakekrevingsbeløp: BigDecimal,
    val skattebeløp: BigDecimal,
    val tilbakekrevingsbeløpEtterSkatt: BigDecimal,
    // Rått beløp, ikke justert for ev. trekk
    val utbetaltYtelsesbeløp: BigDecimal,
    val riktigYtelsesbeløp: BigDecimal,
) {
    operator fun plus(other: Beregningsresultatsperiode) = Beregningsresultatsperiode(
        periode = minOf(periode.fom, other.periode.fom) til maxOf(periode.tom, other.periode.tom),
        vurdering = vurdering,
        feilutbetaltBeløp = feilutbetaltBeløp + other.feilutbetaltBeløp,
        andelAvBeløp = andelAvBeløp,
        renteprosent = renteprosent,
        manueltSattTilbakekrevingsbeløp = sumNullable(manueltSattTilbakekrevingsbeløp, other.manueltSattTilbakekrevingsbeløp),
        tilbakekrevingsbeløpUtenRenter = tilbakekrevingsbeløpUtenRenter + other.tilbakekrevingsbeløpUtenRenter,
        rentebeløp = rentebeløp + other.rentebeløp,
        tilbakekrevingsbeløp = tilbakekrevingsbeløp + other.tilbakekrevingsbeløp,
        skattebeløp = skattebeløp + other.skattebeløp,
        tilbakekrevingsbeløpEtterSkatt = tilbakekrevingsbeløpEtterSkatt + other.tilbakekrevingsbeløpEtterSkatt,
        utbetaltYtelsesbeløp = utbetaltYtelsesbeløp + other.utbetaltYtelsesbeløp,
        riktigYtelsesbeløp = riktigYtelsesbeløp + other.riktigYtelsesbeløp,
    )

    private fun sumNullable(
        a: BigDecimal?,
        b: BigDecimal?,
    ): BigDecimal? {
        return when {
            a == null -> b
            b == null -> a
            else -> a + b
        }
    }
}
