package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import javax.validation.constraints.Size

data class HenleggelsesbrevFritekstDto(val behandlingsresultatstype: Behandlingsresultatstype,
                                       val begrunnelse: String,
                                       @Size(max = 1500, message = "Fritekst er for lang")
                                       val fritekst: String? = null)
