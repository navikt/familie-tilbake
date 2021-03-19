package no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto

import java.time.LocalDate
import java.time.YearMonth

data class HbPeriode(val fom: LocalDate,
                     val tom: LocalDate) {
    constructor(fom: YearMonth, tom: YearMonth): this(fom.atDay(1), tom.atEndOfMonth())

}
