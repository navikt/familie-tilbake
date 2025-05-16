package no.nav.tilbakekreving.beregning.delperiode

import no.nav.tilbakekreving.beregning.HUNDRE_PROSENT
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.math.RoundingMode

class Andel(
    val kravgrunnlagPeriode: KravgrunnlagPeriodeAdapter,
    val delperiode: Datoperiode,
) {
    private val andel = BigDecimal.ONE

    fun feilutbetaltBeløp(): BigDecimal {
        return (kravgrunnlagPeriode.feilutbetaltYtelsesbeløp() * andel).setScale(0, RoundingMode.HALF_DOWN)
    }

    fun riktigYtelsesbeløp(): BigDecimal {
        return (kravgrunnlagPeriode.riktigYteslesbeløp() * andel).setScale(0, RoundingMode.HALF_DOWN)
    }

    fun utbetaltYtelsesbeløp(): BigDecimal {
        return (kravgrunnlagPeriode.utbetaltYtelsesbeløp() * andel).setScale(0, RoundingMode.HALF_DOWN)
    }

    fun skatt(): BigDecimal {
        return kravgrunnlagPeriode.beløpTilbakekreves()
            .sumOf {
                it.beløp()
                    .multiply(andel)
                    .multiply(it.skatteprosent())
                    .divide(HUNDRE_PROSENT)
            }
    }
}
