package no.nav.familie.tilbake.micrometer.domain

import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem

class BehandlingerPerSteg(
    val fagsystem: Fagsystem,
    val behandlingssteg: Behandlingssteg,
    val antall: Int,
)
