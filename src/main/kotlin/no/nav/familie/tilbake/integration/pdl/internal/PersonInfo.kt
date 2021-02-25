package no.nav.familie.tilbake.integration.pdl.internal

import java.time.LocalDate

data class PersonInfo(
        val fødselsdato: LocalDate,
        val navn: String,
        val kjønn: Kjønn
)
