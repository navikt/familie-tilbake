package no.nav.tilbakekreving.api.v1.dto

import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import java.time.LocalDate

data class BehandlingPåVentDto(
    val venteårsak: Venteårsak,
    val tidsfrist: LocalDate,
    val begrunnelse: String?,
)
