package no.nav.tilbakekreving.beregning.delperiode

import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.beregning.delperiode.Delperiode.Beløp.Companion.forKlassekode
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.math.RoundingMode

sealed class Delperiode<B : Delperiode.Beløp>(
    private val kravgrunnlagPeriode: KravgrunnlagPeriodeAdapter,
    private val beløp: List<B>,
) {
    abstract val periode: Datoperiode

    abstract fun renter(): BigDecimal

    abstract fun tilbakekrevesBruttoMedRenter(): BigDecimal

    fun beløp(): List<B> = beløp

    fun feilutbetaltBeløp(): BigDecimal = kravgrunnlagPeriode.feilutbetaltYtelsesbeløp().setScale(0, RoundingMode.HALF_UP)

    fun summer(hentBeløp: B.() -> BigDecimal) = beløp().sumOf { it.hentBeløp() }

    fun beløpForKlassekode(klassekode: String): B = beløp().forKlassekode(klassekode)

    abstract class Beløp(
        val klassekode: String,
        val periode: Datoperiode,
        protected val beløpTilbakekreves: KravgrunnlagPeriodeAdapter.BeløpTilbakekreves,
    ) {
        abstract fun tilbakekrevesBrutto(): BigDecimal

        abstract fun skatt(): BigDecimal

        fun utbetaltYtelsesbeløp(): BigDecimal = beløpTilbakekreves.utbetaltYtelsesbeløp()

        fun riktigYtelsesbeløp(): BigDecimal = beløpTilbakekreves.riktigYteslesbeløp()

        companion object {
            fun <T : Beløp> Iterable<T>.forKlassekode(klassekode: String) = single { it.klassekode == klassekode }
        }
    }
}
