package no.nav.tilbakekreving

import java.time.LocalDate
import java.time.LocalDateTime

interface Klokke {
    fun nå(): LocalDateTime

    fun dagensDato(): LocalDate
}

object SystemKlokke : Klokke {
    override fun nå(): LocalDateTime = LocalDateTime.now()

    override fun dagensDato(): LocalDate = LocalDate.now()
}
