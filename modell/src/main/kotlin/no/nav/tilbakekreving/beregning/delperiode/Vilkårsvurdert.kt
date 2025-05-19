package no.nav.tilbakekreving.beregning.delperiode

import no.nav.tilbakekreving.beregning.HUNDRE_PROSENT
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurdertPeriodeAdapter
import no.nav.tilbakekreving.beregning.fraksjon
import no.nav.tilbakekreving.beregning.isZero
import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.math.RoundingMode

class Vilkårsvurdert(
    override val periode: Datoperiode,
    override val vurdertPeriode: Datoperiode,
    override val andel: Andel,
    private val vurdering: VilkårsvurdertPeriodeAdapter,
    private val beregnRenter: Boolean,
    private val antallKravgrunnlagGjelder: Int,
) : Delperiode {
    private var tilbakekrevingsbeløpAvrunding = BigDecimal.ZERO
    private val tilbakekrevingsbeløp = when {
        vurdering.ignoreresPgaLavtBeløp() -> BigDecimal.ZERO
        else -> vurdering.reduksjon().beregn(andel.feilutbetaltBeløp(), antallKravgrunnlagGjelder)
    }

    private var skattebeløpAvrunding = BigDecimal.ZERO
    private val skattebeløp = beregnSkattebeløp(tilbakekrevingsbeløp)

    private var rentebeløpAvrunding = BigDecimal.ZERO
    private val rentebeløp = beregnRentebeløp(tilbakekrevingsbeløp)

    override fun tilbakekrevesBrutto(): BigDecimal =
        tilbakekrevingsbeløp.setScale(0, RoundingMode.DOWN) + tilbakekrevingsbeløpAvrunding

    override fun tilbakekrevesBruttoMedRenter(): BigDecimal = tilbakekrevesBrutto() + renter()

    private fun tilbakekrevesNetto(): BigDecimal = tilbakekrevesBruttoMedRenter() - skatt()

    override fun renter(): BigDecimal = rentebeløp.setScale(0, RoundingMode.DOWN) + rentebeløpAvrunding

    override fun skatt(): BigDecimal = skattebeløp.setScale(0, RoundingMode.DOWN) + skattebeløpAvrunding

    override fun beregningsresultat(): Beregningsresultatsperiode {
        return Beregningsresultatsperiode(
            periode = periode,
            vurdering = vurdering.vurdering(),
            renteprosent = if (beregnRenter && vurdering.renter()) RENTESATS else null,
            feilutbetaltBeløp = andel.feilutbetaltBeløp(),
            riktigYtelsesbeløp = andel.riktigYtelsesbeløp(),
            utbetaltYtelsesbeløp = andel.utbetaltYtelsesbeløp(),
            andelAvBeløp = vurdering.reduksjon().andel,
            manueltSattTilbakekrevingsbeløp = tilbakekrevingsbeløp.takeIf { vurdering.reduksjon() is Reduksjon.ManueltBeløp }
                ?.setScale(2, RoundingMode.DOWN),
            tilbakekrevingsbeløpUtenRenter = tilbakekrevesBrutto(),
            rentebeløp = renter(),
            tilbakekrevingsbeløpEtterSkatt = tilbakekrevesNetto(),
            skattebeløp = skatt(),
            tilbakekrevingsbeløp = tilbakekrevesBruttoMedRenter(),
        )
    }

    private fun beregnRentebeløp(
        beløp: BigDecimal,
    ): BigDecimal = if (beregnRenter && vurdering.renter()) {
        beløp.multiply(RENTEFAKTOR)
    } else {
        BigDecimal.ZERO
    }

    private fun beregnSkattebeløp(
        tilbakekrevesBeløp: BigDecimal,
    ): BigDecimal {
        if (tilbakekrevesBeløp.isZero()) return BigDecimal.ZERO
        val forskjellMedRenter = tilbakekrevesBeløp.divide(andel.feilutbetaltBeløp(), 4, RoundingMode.HALF_UP)
        return andel.skatt() * forskjellMedRenter
    }

    companion object {
        private val RENTESATS = BigDecimal.valueOf(10)
        private val RENTEFAKTOR = RENTESATS.divide(HUNDRE_PROSENT, 4, RoundingMode.HALF_DOWN)

        fun opprett(
            vurdering: VilkårsvurdertPeriodeAdapter,
            kravgrunnlagPeriode: KravgrunnlagPeriodeAdapter,
            beregnRenter: Boolean,
            antallKravgrunnlagGjelder: Int,
        ): Vilkårsvurdert {
            val delperiode = requireNotNull(kravgrunnlagPeriode.periode().snitt(vurdering.periode())) {
                "Finner ingen kravgrunnlagsperiode som er dekket av vilkårsvurderingsperioden ${vurdering.periode()}, kravgrunnlagsperiode=${kravgrunnlagPeriode.periode()}"
            }
            return Vilkårsvurdert(
                periode = delperiode,
                vurdertPeriode = vurdering.periode(),
                andel = Andel(
                    kravgrunnlagPeriode = kravgrunnlagPeriode,
                    delperiode = delperiode,
                ),
                vurdering = vurdering,
                beregnRenter = beregnRenter,
                antallKravgrunnlagGjelder = antallKravgrunnlagGjelder,
            )
        }

        fun <T : Iterable<Delperiode>> T.fordelTilbakekrevingsbeløp(): T = apply {
            fordel(Vilkårsvurdert::tilbakekrevingsbeløp, RoundingMode.HALF_DOWN) {
                tilbakekrevingsbeløpAvrunding = BigDecimal.ONE
            }
        }

        fun <T : Iterable<Delperiode>> T.fordelRentebeløp(): T = apply {
            fordel(Vilkårsvurdert::rentebeløp, RoundingMode.DOWN) {
                rentebeløpAvrunding = BigDecimal.ONE
            }
        }

        fun <T : Iterable<Delperiode>> T.fordelSkattebeløp(): T = apply {
            fordel(Vilkårsvurdert::skattebeløp, RoundingMode.DOWN) {
                skattebeløpAvrunding = BigDecimal.ONE
            }
        }

        private fun Iterable<Delperiode>.fordel(
            verdi: Vilkårsvurdert.() -> BigDecimal,
            avrunding: RoundingMode,
            økAvrunding: Vilkårsvurdert.() -> Unit,
        ) {
            val overflødigeKronerEtterAvrunding = filterIsInstance<Vilkårsvurdert>().sumOf { it.verdi().fraksjon() }
                .setScale(0, avrunding)
                .toInt()

            val høyesteFraksjon = compareByDescending<Vilkårsvurdert> { it.verdi().fraksjon() }
                .thenBy { it.periode.fom }

            filterIsInstance<Vilkårsvurdert>()
                .sortedWith(høyesteFraksjon)
                .take(overflødigeKronerEtterAvrunding)
                .forEach { it.økAvrunding() }
        }
    }
}
