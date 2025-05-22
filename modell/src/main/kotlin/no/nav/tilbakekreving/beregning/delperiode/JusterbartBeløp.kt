package no.nav.tilbakekreving.beregning.delperiode

import no.nav.tilbakekreving.beregning.HUNDRE_PROSENT
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurdertPeriodeAdapter
import no.nav.tilbakekreving.beregning.isZero
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.math.RoundingMode

class JusterbartBeløp(
    override val klassekode: String,
    override val periode: Datoperiode,
    private val beløpTilbakekreves: KravgrunnlagPeriodeAdapter.BeløpTilbakekreves,
    vurdering: VilkårsvurdertPeriodeAdapter,
    antallKravgrunnlagGjelder: Int,
) : Delperiode.Beløp {
    private var tilbakekrevingsbeløpAvrunding = BigDecimal.ZERO
    val tilbakekrevingsbeløp = when {
        vurdering.ignoreresPgaLavtBeløp() -> BigDecimal.ZERO
        else -> vurdering.reduksjon().beregn(beløpTilbakekreves.tilbakekrevesBeløp(), antallKravgrunnlagGjelder)
    }

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

    override fun skatt(): BigDecimal = skattebeløp.setScale(0, RoundingMode.DOWN) + skattebeløpAvrunding

    override fun utbetaltYtelsesbeløp(): BigDecimal = beløpTilbakekreves.utbetaltYtelsesbeløp()

    override fun riktigYtelsesbeløp(): BigDecimal = beløpTilbakekreves.riktigYteslesbeløp()

    companion object {
        fun Iterable<JusterbartBeløp>.fordelTilbakekrevingsbeløp() {
            fordel(JusterbartBeløp::tilbakekrevingsbeløp, JusterbartBeløp::periode, RoundingMode.HALF_DOWN) {
                tilbakekrevingsbeløpAvrunding = BigDecimal.ONE
            }
        }

        fun Iterable<JusterbartBeløp>.fordelSkattebeløp() {
            fordel(JusterbartBeløp::skattebeløp, JusterbartBeløp::periode, RoundingMode.DOWN) {
                skattebeløpAvrunding = BigDecimal.ONE
            }
        }
    }
}
