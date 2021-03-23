package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype

data class HenleggelsesbrevFritekstDto (val behandlingsresultatstype: Behandlingsresultatstype,
                                        val begrunnelse: String,
                                        val fritekst: String? = null)
