package no.nav.familie.tilbake.dokumentbestilling.handlebars.dto

import no.nav.familie.tilbake.common.Periode
import java.time.LocalDate
import java.time.YearMonth

data class Handlebarsperiode(
    val fom: LocalDate,
    val tom: LocalDate
) {

    constructor(fom: YearMonth, tom: YearMonth) : this(fom.atDay(1), tom.atEndOfMonth())

    constructor(periode: Periode) : this(periode.fom, periode.tom)
}
