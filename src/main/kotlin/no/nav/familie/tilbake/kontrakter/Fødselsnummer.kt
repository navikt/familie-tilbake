package no.nav.familie.tilbake.kontrakter

import java.time.LocalDate

data class Fødselsnummer(
    val verdi: String,
) {
    init {
        check(gyldig(), verdi::toString)
    }

    val erDNummer get() = verdi.substring(0, 1).toInt() > 3
    val erNAVSyntetisk get() = verdi.substring(2, 3).toInt() >= 4 && verdi.substring(2, 3).toInt() < 8
    val erSkattSyntetisk get() = verdi.substring(2, 3).toInt() >= 8

    @Deprecated("Denne kalkulerer fødselsdato basert på ident. Dette kan bli feil.")
    val fødselsdato get() = beregnFødselsdato()

    private fun beregnFødselsdato(): LocalDate {
        val dag = verdi.substring(0, 2).toInt() - (if (erDNummer) 40 else 0)
        val måned =
            verdi.substring(2, 4).toInt() - (
                if (erNAVSyntetisk) {
                    40
                } else if (erSkattSyntetisk) {
                    80
                } else {
                    0
                }
            )
        val år = verdi.substring(4, 6).toInt()
        val datoUtenÅrhundre = LocalDate.of(år, måned, dag)
        val individnummer = verdi.substring(6, 9).toInt()
        when {
            individnummer in 0..499 -> return datoUtenÅrhundre.plusYears(1900)
            individnummer in 500..749 && år >= 54 && år <= 99 -> return datoUtenÅrhundre.plusYears(1800)
            individnummer in 900..999 && år >= 40 && år <= 99 -> return datoUtenÅrhundre.plusYears(1900)
            individnummer in 500..999 && år >= 0 && år <= 39 -> return datoUtenÅrhundre.plusYears(2000)
        }
        throw IllegalArgumentException()
    }

    private fun gyldig(): Boolean {
        if (verdi.length != 11 || verdi.toLongOrNull() == null) {
            return false
        }

        val siffer = verdi.chunked(1).map { it.toInt() }
        val k1Vekting = intArrayOf(3, 7, 6, 1, 8, 9, 4, 5, 2)
        val k2Vekting = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)

        val kontrollMod1 = 11 - (0..8).sumOf { k1Vekting[it] * siffer[it] } % 11
        val kontrollMod2 = 11 - (0..9).sumOf { k2Vekting[it] * siffer[it] } % 11
        val kontrollsiffer1 = siffer[9]
        val kontrollsiffer2 = siffer[10]

        return gyldigKontrollSiffer(kontrollMod1, kontrollsiffer1) && gyldigKontrollSiffer(kontrollMod2, kontrollsiffer2)
    }

    private fun gyldigKontrollSiffer(
        kontrollMod: Int,
        kontrollsiffer: Int,
    ): Boolean {
        if (kontrollMod == kontrollsiffer) {
            return true
        }
        if (kontrollMod == 11 && kontrollsiffer == 0) {
            return true
        }
        return false
    }

    override fun toString(): String = "***********"
}
