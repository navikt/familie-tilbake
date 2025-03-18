package no.nav.tilbakekreving.api.v1.dto

import jakarta.validation.constraints.Size
import no.nav.tilbakekreving.kontrakter.brev.Dokumentmalstype
import java.util.UUID

class BestillBrevDto(
    val behandlingId: UUID,
    val brevmalkode: Dokumentmalstype,
    @Size(min = 1, max = 3000)
    val fritekst: String,
)
