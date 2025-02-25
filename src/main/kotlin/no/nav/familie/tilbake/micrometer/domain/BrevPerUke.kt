package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.kontrakter.Fagsystem

class BrevPerUke(
    val år: Int,
    val uke: Int,
    val fagsystem: Fagsystem,
    val brevtype: Brevtype,
    val antall: Int,
)
