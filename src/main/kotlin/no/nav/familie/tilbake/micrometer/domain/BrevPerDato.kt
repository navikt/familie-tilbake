package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import java.time.LocalDate

class BrevPerDato(val dato: LocalDate,
                  val ytelsestype: Ytelsestype,
                  val brevtype: Brevtype,
                  val antall: Int)
