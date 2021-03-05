package no.nav.familie.tilbake.common

import java.time.LocalDate

data class Periode(val fom: LocalDate,
                   val tom: LocalDate) : Comparable<Periode> {

    init {
        require(tom >= fom) { "Til-og-med-dato før fra-og-med-dato: $fom>$tom" }
    }

    private fun overlapper(dato: LocalDate): Boolean {
        return dato in fom..tom
    }

    private fun overlapper(other: Periode): Boolean {
        return overlapper(other.fom) || overlapper(other.tom)
    }

    fun union(annen: Periode): Periode? {
        return if (!overlapper(annen)) {
            null
        } else if (this == annen) {
            this
        } else {
            Periode(maxOf(fom, annen.fom),
                    minOf(tom, annen.tom))
        }
    }

    companion object {

        val COMPARATOR: Comparator<Periode> = Comparator.comparing(Periode::fom).thenComparing(Periode::tom)
    }

    override fun compareTo(other: Periode): Int {
        return COMPARATOR.compare(this, other)
    }

}
