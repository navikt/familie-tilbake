package no.nav.tilbakekreving.beregning.delperiode

import no.nav.tilbakekreving.beregning.fraksjon
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.math.RoundingMode

inline fun <reified T> Iterable<*>.fordel(
    totalbeløp: BigDecimal,
    crossinline verdi: T.() -> BigDecimal,
    crossinline periode: T.() -> Datoperiode,
    økAvrunding: T.() -> Unit,
) {
    val verdier = filterIsInstance<T>()
    val heleKroner = verdier.sumOf { it.verdi().setScale(0, RoundingMode.DOWN) }
    val overflødigeKroner = totalbeløp.subtract(heleKroner).intValueExact()

    val høyesteFraksjon = compareByDescending<T> { it.verdi().fraksjon() }
        .thenBy { it.periode().fom }

    verdier
        .sortedWith(høyesteFraksjon)
        .take(overflødigeKroner)
        .forEach { it.økAvrunding() }
}
