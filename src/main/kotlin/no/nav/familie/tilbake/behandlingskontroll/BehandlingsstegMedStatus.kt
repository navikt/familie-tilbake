package no.nav.familie.tilbake.behandlingskontroll

import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus

data class BehandlingsstegMedStatus(val behandlingssteg: Behandlingssteg,
                                    val behandlingsstegstatus: Behandlingsstegstatus)
