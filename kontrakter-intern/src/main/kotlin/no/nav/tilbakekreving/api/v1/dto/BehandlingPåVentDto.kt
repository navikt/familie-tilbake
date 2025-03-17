package no.nav.tilbakekreving.api.v1.dto

import java.time.LocalDate
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak

data class BehandlingPåVentDto(
    val venteårsak: Venteårsak,
    val tidsfrist: LocalDate,
)
