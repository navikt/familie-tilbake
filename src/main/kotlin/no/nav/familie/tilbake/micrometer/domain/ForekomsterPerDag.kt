package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import java.time.LocalDate

class ForekomsterPerDag(val dato: LocalDate,
                        val ytelsestype: Ytelsestype,
                        val antall: Int)
