package no.nav.tilbakekreving.beregning.delperiode

import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal

sealed interface Delperiode {
    val periode: Datoperiode
    val vurdertPeriode: Datoperiode

    fun renter(): BigDecimal

    fun tilbakekrevesBruttoMedRenter(): BigDecimal

    fun feilutbetaltBeløp(): BigDecimal

    fun beløp(): List<Beløp>

    fun beløpForKlassekode(klassekode: String): Beløp

    interface Beløp {
        val klassekode: String

        val periode: Datoperiode

        fun tilbakekrevesBrutto(): BigDecimal

        fun riktigYtelsesbeløp(): BigDecimal

        fun utbetaltYtelsesbeløp(): BigDecimal

        fun skatt(): BigDecimal

        companion object {
            fun Iterable<Beløp>.forKlassekode(klassekode: String) = single { it.klassekode == klassekode }
        }
    }
}
