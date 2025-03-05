package no.nav.familie.tilbake.micrometer.domain

import no.nav.tilbakekreving.kontrakter.Fagsystem

class ForekomsterPerUke(
    val år: Int,
    val uke: Int,
    val fagsystem: Fagsystem,
    val antall: Int,
)
