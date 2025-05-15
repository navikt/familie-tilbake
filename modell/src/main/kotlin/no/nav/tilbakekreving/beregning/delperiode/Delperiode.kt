package no.nav.tilbakekreving.beregning.delperiode

import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal

sealed interface Delperiode {
    val andel: Andel
    val periode: Datoperiode
    val vurdertPeriode: Datoperiode

    fun beregningsresultat(): Beregningsresultatsperiode

    fun tilbakekrevesBrutto(): BigDecimal

    fun tilbakekrevesBruttoMedRenter(): BigDecimal

    fun renter(): BigDecimal

    fun skatt(): BigDecimal

    companion object {
        fun Iterable<Delperiode>.oppsummer() = groupBy { it.vurdertPeriode }
            .mapValues { (_, perioder) -> perioder.map { it.beregningsresultat() }.reduce(Beregningsresultatsperiode::plus) }
            .values
            .toList()
    }
}
