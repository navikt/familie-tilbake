package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.time.LocalDate

@Serializable
data class DatoperiodeEntity(
    val fom: String,
    val tom: String,
) {
    fun fraEntity(): Datoperiode {
        return Datoperiode(LocalDate.parse(fom), LocalDate.parse(tom))
    }
}
