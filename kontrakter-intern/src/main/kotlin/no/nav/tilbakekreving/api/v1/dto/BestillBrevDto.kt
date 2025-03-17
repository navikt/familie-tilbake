package no.nav.tilbakekreving.api.v1.dto

import jakarta.validation.constraints.Size
import java.util.UUID
import no.nav.tilbakekreving.kontrakter.brev.Dokumentmalstype

class BestillBrevDto(
    val behandlingId: UUID,
    val brevmalkode: Dokumentmalstype,
    @Size(min = 1, max = 3000)
    val fritekst: String,
)
