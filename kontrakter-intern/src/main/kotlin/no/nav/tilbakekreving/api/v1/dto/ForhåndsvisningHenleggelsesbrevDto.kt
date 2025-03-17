package no.nav.tilbakekreving.api.v1.dto

import jakarta.validation.constraints.Size
import java.util.UUID

data class ForhåndsvisningHenleggelsesbrevDto(
    val behandlingId: UUID,
    @Size(max = 1500, message = "Fritekst er for lang")
    val fritekst: String?,
)
