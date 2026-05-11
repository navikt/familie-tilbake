package no.nav.tilbakekreving.behandling.saksbehandling

import java.time.LocalDate

data class Venter(
    val grunn: Grunn,
    val frist: LocalDate,
) {
    enum class Grunn {
        BRUKERUTTALELSE,
    }
}
