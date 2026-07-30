package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.time.LocalDate

data class DatoperiodeEntity(
    val fom: LocalDate,
    val tom: LocalDate,
) : Comparable<DatoperiodeEntity> {
    fun fraEntity(): Datoperiode {
        return Datoperiode(fom, tom)
    }

    override fun compareTo(other: DatoperiodeEntity): Int {
        return fom.compareTo(other.fom)
    }
}
