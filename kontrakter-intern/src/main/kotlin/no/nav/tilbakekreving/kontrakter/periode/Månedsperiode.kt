package no.nav.tilbakekreving.kontrakter.periode

import java.time.LocalDate
import java.time.YearMonth

data class Månedsperiode(
    override val fom: YearMonth,
    override val tom: YearMonth,
) : Periode<YearMonth>() {
    init {
        validate()
    }

    val fomDato get() = fom.atDay(1)
    val tomDato get() = tom.atEndOfMonth()

    constructor(fom: LocalDate, tom: LocalDate) : this(YearMonth.from(fom), YearMonth.from(tom))

    constructor(måned: YearMonth) : this(måned, måned)
    constructor(måned: String) : this(YearMonth.parse(måned))
    constructor(fom: String, tom: String) : this(YearMonth.parse(fom), YearMonth.parse(tom))
    constructor(periode: Pair<String, String>) : this(periode.first, periode.second)

    override fun lagPeriode(
        fom: YearMonth,
        tom: YearMonth,
    ): Månedsperiode = Månedsperiode(fom, tom)

    override infix fun union(annen: Periode<YearMonth>): Månedsperiode = super.union(annen) as Månedsperiode

    override infix fun snitt(annen: Periode<YearMonth>): Månedsperiode? = super.snitt(annen) as Månedsperiode?

    override infix fun påfølgesAv(påfølgende: Periode<YearMonth>): Boolean = this.tom.plusMonths(1) == påfølgende.fom

    override fun lengdeIHeleMåneder(): Long = (tom.year * 12 + tom.monthValue) - (fom.year * 12 + fom.monthValue) + 1L

    fun toDatoperiode() = Datoperiode(fomDato, tomDato)
}
