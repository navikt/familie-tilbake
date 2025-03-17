package no.nav.familie.tilbake.micrometer.domain

import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsresultatstype
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem

class VedtakPerUke(
    val år: Int,
    val uke: Int,
    val fagsystem: Fagsystem,
    val vedtakstype: Behandlingsresultatstype,
    val antall: Int,
)
