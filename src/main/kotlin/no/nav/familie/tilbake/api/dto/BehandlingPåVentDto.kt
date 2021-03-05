package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import java.time.LocalDate
import java.util.UUID

data class BehandlingPåVentDto(val behandlingId: UUID,
                               val venteårsak: Venteårsak,
                               val tidsfrist: LocalDate)
