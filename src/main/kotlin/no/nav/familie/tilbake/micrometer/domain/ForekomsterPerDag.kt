package no.nav.familie.tilbake.micrometer.domain

import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import java.time.LocalDate

class ForekomsterPerDag(
    val dato: LocalDate,
    val fagsystem: Fagsystem,
    val antall: Int,
)
