package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.common.Periode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class PeriodeDto(val fom: LocalDate, val tom: LocalDate) {

    constructor(fom: YearMonth, tom: YearMonth) : this(fom.atDay(1), tom.atEndOfMonth())

    constructor(periode: Periode) : this(periode.fom, periode.tom)
}

data class BeregnetPeriodeDto(val periode: PeriodeDto, val feilutbetaltBeløp: BigDecimal)

data class BeregnetPerioderDto(val beregnetPerioder: List<BeregnetPeriodeDto>)
