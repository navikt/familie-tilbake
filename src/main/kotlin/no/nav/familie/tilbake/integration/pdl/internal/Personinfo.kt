package no.nav.familie.tilbake.integration.pdl.internal

import java.time.LocalDate

data class Personinfo(
    val ident: String,
    val fødselsdato: LocalDate,
    val navn: String,
    val kjønn: PdlKjønnType = PdlKjønnType.UKJENT,
    val dødsdato: LocalDate? = null,
)
