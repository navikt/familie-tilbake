package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.tilbakekreving.kontrakter.Fagsystem

class VedtakPerUke(
    val år: Int,
    val uke: Int,
    val fagsystem: Fagsystem,
    val vedtakstype: Behandlingsresultatstype,
    val antall: Int,
)
