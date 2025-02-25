package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.kontrakter.tilbakekreving.Brevmottaker
import java.util.UUID

typealias ManuellBrevmottakerRequestDto = Brevmottaker

data class ManuellBrevmottakerResponsDto(
    val id: UUID,
    val brevmottaker: Brevmottaker,
)
