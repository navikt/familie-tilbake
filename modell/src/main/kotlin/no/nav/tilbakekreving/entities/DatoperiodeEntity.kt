package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.time.LocalDate

data class DatoperiodeEntity(
    val fom: LocalDate,
    val tom: LocalDate,
) {
    fun fraEntity(): Datoperiode {
        return Datoperiode(fom, tom)
    }
}
