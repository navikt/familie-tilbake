package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype

class ForekomsterPerUke(val år: Int,
                        val uke: Int,
                        val ytelsestype: Ytelsestype,
                        val antall: Int)
