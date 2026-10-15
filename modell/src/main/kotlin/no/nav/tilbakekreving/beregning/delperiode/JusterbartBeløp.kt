package no.nav.tilbakekreving.beregning.delperiode

import no.nav.tilbakekreving.beregning.HUNDRE_PROSENT
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.beregning.isZero
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.math.RoundingMode

class JusterbartBeløp(
    klassekode: String,
    periode: Datoperiode,
    beløpTilbakekreves: KravgrunnlagPeriodeAdapter.BeløpTilbakekreves,
    reduksjon: Reduksjon,
    totaltFeilutbetaltBeløp: BigDecimal,
) : Delperiode.Beløp(klassekode, periode, beløpTilbakekreves) {
    private var tilbakekrevingsbeløpAvrunding = BigDecimal.ZERO

    val tilbakekrevingsbeløp: BigDecimal = reduksjon.beregn(totaltFeilutbetaltBeløp)
        .multiply(beløpTilbakekreves.tilbakekrevesBeløp())
        // Deler til slutt for å beholde presisjon så lenge som mulig
        .divide(totaltFeilutbetaltBeløp, 10, RoundingMode.HALF_DOWN)

    private var skattebeløpAvrunding = BigDecimal.ZERO
    private val skattebeløp = beregnSkattebeløp(tilbakekrevingsbeløp)

    private fun beregnSkattebeløp(
        tilbakekrevesBeløp: BigDecimal,
    ): BigDecimal {
        if (tilbakekrevesBeløp.isZero()) return BigDecimal.ZERO
        val forskjellMedRenter = tilbakekrevesBeløp.divide(beløpTilbakekreves.tilbakekrevesBeløp(), 4, RoundingMode.HALF_UP)

        val antallKronerSkatt = beløpTilbakekreves.tilbakekrevesBeløp()
            .multiply(beløpTilbakekreves.skatteprosent())
            .divide(HUNDRE_PROSENT)

        return antallKronerSkatt.multiply(forskjellMedRenter)
    }

    override fun tilbakekrevesBrutto(): BigDecimal = tilbakekrevingsbeløp.setScale(0, RoundingMode.DOWN) + tilbakekrevingsbeløpAvrunding

    fun tilbakekreverMerEnnFeilutbetalt(): Boolean = tilbakekrevesBrutto() > beløpTilbakekreves.tilbakekrevesBeløp()

    override fun skatt(): BigDecimal = skattebeløp.setScale(0, RoundingMode.DOWN) + skattebeløpAvrunding

    companion object {
        fun Iterable<JusterbartBeløp>.fordelTilbakekrevingsbeløp(totaltTilbakekrevingsbeløp: BigDecimal) {
            fordel(totaltTilbakekrevingsbeløp, JusterbartBeløp::tilbakekrevingsbeløp, JusterbartBeløp::periode) {
                tilbakekrevingsbeløpAvrunding = BigDecimal.ONE
            }
        }

        fun Iterable<JusterbartBeløp>.fordelSkattebeløp() {
            val totaltSkattebeløp = sumOf { it.skattebeløp }.setScale(0, RoundingMode.HALF_UP)
            fordel(totaltSkattebeløp, JusterbartBeløp::skattebeløp, JusterbartBeløp::periode) {
                skattebeløpAvrunding = BigDecimal.ONE
            }
        }
    }
}
