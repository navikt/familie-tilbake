package no.nav.tilbakekreving.behandling.saksbehandling

import java.time.LocalDate

data class Venter(
    val grunn: Grunn,
    val frist: LocalDate,
) {
    enum class Grunn {
        BRUKERUTTALELSE,
        SPERRET_VENTER_PÅ_OPPDATERING_FRA_ØKONOMI,
    }
}
