package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.kontrakter.Fagsystem

class BehandlingerPerSteg(
    val fagsystem: Fagsystem,
    val behandlingssteg: Behandlingssteg,
    val antall: Int,
)
