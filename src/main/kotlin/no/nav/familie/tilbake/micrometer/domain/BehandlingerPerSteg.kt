package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg

class BehandlingerPerSteg(
    val fagsystem: Fagsystem,
    val behandlingssteg: Behandlingssteg,
    val antall: Int,
)
