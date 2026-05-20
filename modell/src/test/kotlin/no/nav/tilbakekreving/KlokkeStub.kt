package no.nav.tilbakekreving

import java.time.LocalDate
import java.time.LocalDateTime

class KlokkeStub(
    private var nå: LocalDateTime,
) : Klokke {
    constructor(dagensDato: LocalDate) : this(dagensDato.atStartOfDay())

    fun settTid(nyTid: LocalDate) {
        nå = nyTid.atStartOfDay()
    }

    override fun nå(): LocalDateTime = nå

    override fun dagensDato(): LocalDate = nå.toLocalDate()
}
