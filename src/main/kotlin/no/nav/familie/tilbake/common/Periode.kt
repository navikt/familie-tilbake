package no.nav.familie.tilbake.common

import java.time.LocalDate

data class Periode(val fom: LocalDate,
                   val tom: LocalDate) {

    init {
        require(!tom.isBefore(fom)) { "Til-og-med-dato før fra-og-med-dato: $fom>$tom" }
    }

    fun overlapper(dato: LocalDate): Boolean {
        return !dato.isBefore(fom) && !dato.isAfter(tom)
    }

    fun overlapper(other: Periode): Boolean {
       return  overlapper(other.fom) || overlapper(other.tom)
    }

    fun union(annen: Periode): Periode? {
        return if (!overlapper(annen)) {
            null
        } else if (this == annen) {
            this
        } else {
            Periode(max(fom, annen.fom),
                    min(tom, annen.tom))
        }
    }



    companion object {
        fun max(en: LocalDate, to: LocalDate): LocalDate {
            return if (en.isAfter(to)) en else to
        }

        fun min(en: LocalDate, to: LocalDate): LocalDate {
            return if (en.isBefore(to)) en else to
        }

        val COMPARATOR: Comparator<Periode> = Comparator.comparing(Periode::fom).thenComparing(Periode::tom)
    }

}
