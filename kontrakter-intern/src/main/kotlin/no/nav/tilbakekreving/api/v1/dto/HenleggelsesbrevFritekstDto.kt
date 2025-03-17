package no.nav.tilbakekreving.api.v1.dto

import jakarta.validation.constraints.Size
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsresultatstype

data class HenleggelsesbrevFritekstDto(
    val behandlingsresultatstype: Behandlingsresultatstype,
    val begrunnelse: String,
    @Size(max = 1500, message = "Fritekst er for lang")
    val fritekst: String? = null,
)
