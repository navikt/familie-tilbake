package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype

class BrevPerUke(val år: Int,
                 val uke: Int,
                 val ytelsestype: Ytelsestype,
                 val brevtype: Brevtype,
                 val antall: Int)
