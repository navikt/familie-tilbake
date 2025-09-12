package no.nav.tilbakekreving.kontrakter.periode

import java.time.LocalDate
import java.time.YearMonth

data class Datoperiode(
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate>() {
    init {
        validate()
    }

    val fomMåned get() = YearMonth.from(fom)
    val tomMåned get() = YearMonth.from(tom)

    constructor(fom: YearMonth, tom: YearMonth) : this(fom.atDay(1), tom.atEndOfMonth())
    constructor(fom: String, tom: String) : this(LocalDate.parse(fom), LocalDate.parse(tom))
    constructor(periode: Pair<String, String>) : this(periode.first, periode.second)

    override fun lagPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): Datoperiode = Datoperiode(fom, tom)

    override infix fun snitt(annen: Periode<LocalDate>): Datoperiode? = super.snitt(annen) as Datoperiode?

    override fun lengdeIHeleMåneder(): Long {
        require(fom.dayOfMonth == 1 && tom == YearMonth.from(tom).atEndOfMonth()) {
            "Forsøk på å beregne lengde i hele måneder for en periode som ikke er hele måneder: $fom - $tom"
        }
        return (tom.year * 12 + tom.monthValue) - (fom.year * 12 + fom.monthValue) + 1L
    }

    operator fun contains(other: Datoperiode) = snitt(other) != null

    override fun equals(other: Any?): Boolean {
        val annenPeriode = other as? Datoperiode ?: return false
        return fom == annenPeriode.fom && tom == annenPeriode.tom
    }

    fun toMånedsperiode() = Månedsperiode(fomMåned, tomMåned)
}

infix fun LocalDate.til(tom: LocalDate) = Datoperiode(this, tom)
