package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype

class VedtakPerUke(val år: Int,
                   val uke: Int,
                   val ytelsestype: Ytelsestype,
                   val vedtakstype: Behandlingsresultatstype,
                   val antall: Int)
