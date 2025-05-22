package no.nav.tilbakekreving.beregning.adapter

import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal

interface KravgrunnlagPeriodeAdapter {
    fun periode(): Datoperiode

    fun feilutbetaltYtelsesbeløp(): BigDecimal

    fun beløpTilbakekreves(): List<BeløpTilbakekreves>

    interface BeløpTilbakekreves {
        fun klassekode(): String

        fun tilbakekrevesBeløp(): BigDecimal

        fun utbetaltYtelsesbeløp(): BigDecimal

        fun riktigYteslesbeløp(): BigDecimal

        fun skatteprosent(): BigDecimal
    }
}
