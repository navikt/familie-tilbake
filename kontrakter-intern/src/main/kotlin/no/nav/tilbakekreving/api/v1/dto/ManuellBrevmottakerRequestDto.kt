package no.nav.tilbakekreving.api.v1.dto

import no.nav.tilbakekreving.kontrakter.brev.Brevmottaker
import java.util.UUID

typealias ManuellBrevmottakerRequestDto = Brevmottaker

data class ManuellBrevmottakerResponsDto(
    val id: UUID,
    val brevmottaker: Brevmottaker,
)
