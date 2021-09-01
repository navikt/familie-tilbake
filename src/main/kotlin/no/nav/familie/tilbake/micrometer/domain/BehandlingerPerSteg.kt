package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg

class BehandlingerPerSteg(val ytelsestype: Ytelsestype,
                          val behandlingssteg: Behandlingssteg,
                          val antall: Int)
