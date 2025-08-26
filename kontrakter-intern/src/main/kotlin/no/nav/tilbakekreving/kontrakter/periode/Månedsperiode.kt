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

    fun før(annenMåned: YearMonth): Månedsperiode? {
        if (this.fom >= annenMåned) return null
        return Månedsperiode(this.fom, annenMåned.minusMonths(1))
    }

    fun etter(annenMåned: YearMonth): Månedsperiode? {
        if (this.tom <= annenMåned) return null
        return Månedsperiode(annenMåned.plusMonths(1), this.tom)
    }

    override infix fun snitt(annen: Periode<YearMonth>): Månedsperiode? = super.snitt(annen) as Månedsperiode?

    override fun lengdeIHeleMåneder(): Long = (tom.year * 12 + tom.monthValue) - (fom.year * 12 + fom.monthValue) + 1L

    fun toDatoperiode() = Datoperiode(fomDato, tomDato)

    companion object {
        infix fun YearMonth.til(tom: YearMonth) = Månedsperiode(this, tom)
    }
}
