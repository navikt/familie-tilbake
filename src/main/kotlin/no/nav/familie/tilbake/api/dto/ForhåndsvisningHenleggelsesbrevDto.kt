package no.nav.familie.tilbake.api.dto

import java.util.UUID
import javax.validation.constraints.Size

data class ForhåndsvisningHenleggelsesbrevDto(
    val behandlingId: UUID,
    @Size(max = 1500, message = "Fritekst er for lang")
    val fritekst: String?
)
