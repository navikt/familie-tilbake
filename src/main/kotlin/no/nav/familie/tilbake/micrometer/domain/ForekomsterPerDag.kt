package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.tilbake.behandling.Fagsystem
import java.time.LocalDate

class ForekomsterPerDag(
    val dato: LocalDate,
    val fagsystem: Fagsystem,
    val antall: Int,
)
