package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.tilbake.kontrakter.Fagsystem

class ForekomsterPerUke(
    val år: Int,
    val uke: Int,
    val fagsystem: Fagsystem,
    val antall: Int,
)
