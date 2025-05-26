package no.nav.tilbakekreving.beregning.delperiode

import no.nav.tilbakekreving.beregning.fraksjon
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.math.RoundingMode

inline fun <reified T> Iterable<*>.fordel(
    crossinline verdi: T.() -> BigDecimal,
    crossinline periode: T.() -> Datoperiode,
    avrunding: RoundingMode,
    økAvrunding: T.() -> Unit,
) {
    val overflødigeKronerEtterAvrunding = filterIsInstance<T>().sumOf { it.verdi().fraksjon() }
        .setScale(0, avrunding)
        .toInt()

    val høyesteFraksjon = compareByDescending<T> { it.verdi().fraksjon() }
        .thenBy { it.periode().fom }

    filterIsInstance<T>()
        .sortedWith(høyesteFraksjon)
        .take(overflødigeKronerEtterAvrunding)
        .forEach { it.økAvrunding() }
}
