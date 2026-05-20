package no.nav.tilbakekreving.fagsystem.events

import java.time.LocalDate

data class VenterEventDto(
    val grunn: VentegrunnEventDto,
    val gjennoptas: LocalDate,
    val gjenopptas: LocalDate,
)
