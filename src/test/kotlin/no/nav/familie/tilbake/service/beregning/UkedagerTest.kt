package no.nav.familie.tilbake.service.beregning

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

class UkedagerTest {

    private val iDag: LocalDate = LocalDate.now()
    private val mandag: LocalDate = iDag.minusDays(iDag.dayOfWeek.value - DayOfWeek.MONDAY.value.toLong())
    private var uke = DayOfWeek.values().associateWith { mandag.plusDays(it.ordinal.toLong()) }


    @Test
    fun `beregnAntallVirkedager skal beregne korrekt antall virkedager`() {
        val mandag = getDayOfWeek(DayOfWeek.MONDAY)
        val søndag = getDayOfWeek(DayOfWeek.SUNDAY)
        assertThat(Ukedager.beregnAntallVirkedager(mandag, søndag)).isEqualTo(5)
        assertThat(Ukedager.beregnAntallVirkedager(mandag, søndag.plusDays(1))).isEqualTo(6)
        assertThat(Ukedager.beregnAntallVirkedager(mandag, søndag.plusDays(10))).isEqualTo(13)
        assertThat(Ukedager.beregnAntallVirkedager(mandag.plusDays(1), søndag)).isEqualTo(4)
        assertThat(Ukedager.beregnAntallVirkedager(mandag.plusDays(1), søndag.plusDays(1))).isEqualTo(5)
        assertThat(Ukedager.beregnAntallVirkedager(mandag.plusDays(4), søndag)).isEqualTo(1)
        assertThat(Ukedager.beregnAntallVirkedager(mandag.plusDays(5), søndag)).isEqualTo(0)
        assertThat(Ukedager.beregnAntallVirkedager(mandag.minusDays(1), søndag)).isEqualTo(5)
        assertThat(Ukedager.beregnAntallVirkedager(mandag.minusDays(2), søndag)).isEqualTo(5)
        assertThat(Ukedager.beregnAntallVirkedager(mandag.minusDays(3), søndag)).isEqualTo(6)
        assertThat(Ukedager.beregnAntallVirkedager(mandag.minusDays(3), søndag.plusDays(1))).isEqualTo(7)
    }

    @Test
    fun `plusVirkedager skal legge til riktig antall dager`() {
        val mandag = getDayOfWeek(DayOfWeek.MONDAY)
        val tirsdag = getDayOfWeek(DayOfWeek.TUESDAY)
        val onsdag = getDayOfWeek(DayOfWeek.WEDNESDAY)
        val fredag = getDayOfWeek(DayOfWeek.FRIDAY)
        val lørdag = getDayOfWeek(DayOfWeek.SATURDAY)
        val søndag = getDayOfWeek(DayOfWeek.SUNDAY)
        val nesteMandag = mandag.plusWeeks(1)
        val nesteTirsdag = tirsdag.plusWeeks(1)
        val nesteOnsdag = onsdag.plusWeeks(1)
        assertThat(Ukedager.plusVirkedager(mandag, 1)).isEqualTo(tirsdag)
        assertThat(Ukedager.plusVirkedager(mandag, 4)).isEqualTo(fredag)
        assertThat(Ukedager.plusVirkedager(mandag, 5)).isEqualTo(nesteMandag)
        assertThat(Ukedager.plusVirkedager(tirsdag, 1)).isEqualTo(onsdag)
        assertThat(Ukedager.plusVirkedager(tirsdag, 3)).isEqualTo(fredag)
        assertThat(Ukedager.plusVirkedager(tirsdag, 4)).isEqualTo(nesteMandag)
        assertThat(Ukedager.plusVirkedager(lørdag, 1)).isEqualTo(nesteTirsdag)
        assertThat(Ukedager.plusVirkedager(fredag, 2)).isEqualTo(nesteTirsdag)
        assertThat(Ukedager.plusVirkedager(lørdag, 2)).isEqualTo(nesteOnsdag)
        assertThat(Ukedager.plusVirkedager(søndag, 5)).isEqualTo(søndag.plusWeeks(1).plusDays(1))
    }

    private fun getDayOfWeek(dayOfWeek: DayOfWeek): LocalDate {
        val date = uke[dayOfWeek] ?: error("Programmeringsfeil.")
        assertThat(date.dayOfWeek).isEqualTo(dayOfWeek)
        return date
    }
}
