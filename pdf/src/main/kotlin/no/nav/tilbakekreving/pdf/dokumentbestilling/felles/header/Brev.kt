package no.nav.tilbakekreving.pdf.dokumentbestilling.felles.header

import java.time.LocalDate

class Brev(
    val overskrift: String?,
    val dato: LocalDate = LocalDate.now(),
)
