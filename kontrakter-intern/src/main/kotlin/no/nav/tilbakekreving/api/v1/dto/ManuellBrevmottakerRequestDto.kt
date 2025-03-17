package no.nav.tilbakekreving.api.v1.dto

import java.util.UUID
import no.nav.tilbakekreving.kontrakter.brev.Brevmottaker

typealias ManuellBrevmottakerRequestDto = Brevmottaker

data class ManuellBrevmottakerResponsDto(
    val id: UUID,
    val brevmottaker: Brevmottaker,
)
