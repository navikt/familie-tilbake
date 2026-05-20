package no.nav.tilbakekreving

import java.time.LocalDate
import java.time.LocalDateTime

class KlokkeStub(
    private val nå: LocalDateTime,
) : Klokke {
    constructor(dagensDato: LocalDate) : this(dagensDato.atStartOfDay())

    override fun nå(): LocalDateTime = nå

    override fun dagensDato(): LocalDate = nå.toLocalDate()
}
