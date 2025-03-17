package no.nav.tilbakekreving.kontrakter.periode

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.Temporal

sealed class Periode<T> : Comparable<Periode<T>> where T : Comparable<T>, T : Temporal {
    abstract val fom: T
    abstract val tom: T

    protected fun validate() {
        require(tom >= fom) { "Til-og-med før fra-og-med: $fom > $tom" }
    }

    infix fun inneholder(dato: T): Boolean = dato in fom..tom

    infix fun inneholder(annen: Periode<T>): Boolean = annen.fom >= this.fom && annen.tom <= this.tom

    infix fun omsluttesAv(annen: Periode<T>): Boolean = annen.fom <= fom && annen.tom >= tom

    infix fun overlapper(other: Periode<T>): Boolean = inneholder(other.fom) || inneholder(other.tom) || other.inneholder(fom)

    open infix fun snitt(annen: Periode<T>): Periode<T>? =
        if (!overlapper(annen)) {
            null
        } else if (this == annen) {
            this
        } else {
            lagPeriode(
                maxOf(fom, annen.fom),
                minOf(tom, annen.tom),
            )
        }

    open infix fun union(annen: Periode<T>): Periode<T> =
        if (overlapper(annen) || this.påfølgesAv(annen) || annen.påfølgesAv(this)) {
            lagPeriode(
                minOf(fom, annen.fom),
                maxOf(tom, annen.tom),
            )
        } else {
            error("Kan ikke lage union av perioder som $this og $annen som ikke overlapper eller direkte følger hverandre.")
        }

    infix fun overlapperKunIStartenAv(annen: Periode<T>) = annen.fom in fom..tom && tom < annen.tom

    infix fun overlapperKunISluttenAv(annen: Periode<T>) = annen.tom in fom..tom && fom > annen.fom

    abstract infix fun påfølgesAv(påfølgende: Periode<T>): Boolean

    abstract fun lengdeIHeleMåneder(): Long

    override fun compareTo(other: Periode<T>): Int = Comparator.comparing(Periode<T>::fom).thenComparing(Periode<T>::tom).compare(this, other)

    abstract fun lagPeriode(
        fom: T,
        tom: T,
    ): Periode<T>
}
