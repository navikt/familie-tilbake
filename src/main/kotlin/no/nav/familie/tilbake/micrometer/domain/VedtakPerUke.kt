package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsresultatstype

class VedtakPerUke(
    val år: Int,
    val uke: Int,
    val fagsystem: Fagsystem,
    val vedtakstype: Behandlingsresultatstype,
    val antall: Int,
)
