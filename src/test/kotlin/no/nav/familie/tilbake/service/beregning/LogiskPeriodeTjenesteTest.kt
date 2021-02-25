package no.nav.familie.tilbake.service.beregning

import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.service.modell.LogiskPeriode
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.SortedMap

class LogiskPeriodeTjenesteTest {

    private val uke1onsdag = LocalDate.of(2020, 1, 1)
    private val uke1torsdag = uke1onsdag.plusDays(1)
    private val uke1fredag = uke1onsdag.plusDays(2)
    private val uke1lørdag = uke1onsdag.plusDays(3)
    private val uke1søndag = uke1onsdag.plusDays(4)
    private val uke2mandag = uke1onsdag.plusDays(5)

    @Test
    fun `tom input skal gi tom output`() {
        val resultat: List<LogiskPeriode> = LogiskPeriodeTjeneste.utledLogiskPeriode(sortedMapOf())
        Assertions.assertThat(resultat).isEmpty()
    }

    @Test
    fun `en periode skal fortsette som samme periode`() {
        val periode1 = Periode(uke1onsdag, uke1lørdag)
        val input: SortedMap<Periode, BigDecimal> = sortedMapOf(periode1 to BigDecimal.ONE)
        val resultat: List<LogiskPeriode> = LogiskPeriodeTjeneste.utledLogiskPeriode(input)
        Assertions.assertThat(resultat).hasSize(1)
        assertSamme(resultat[0], periode1, BigDecimal.ONE)
    }

    @Test
    fun `intilliggende perioder skal slås sammen og summere verdi`() {
        val periode1 = Periode(uke1onsdag, uke1onsdag)
        val periode2 = Periode(uke1torsdag, uke1torsdag)
        val periode3 = Periode(uke1fredag, uke2mandag)
        val input: SortedMap<Periode, BigDecimal> = sortedMapOf(periode1 to BigDecimal.ONE,
                                                                periode2 to BigDecimal.ONE,
                                                                periode3 to BigDecimal.ONE)
        val resultat: List<LogiskPeriode> = LogiskPeriodeTjeneste.utledLogiskPeriode(input)
        Assertions.assertThat(resultat).hasSize(1)
        assertSamme(resultat[0], Periode(periode1.fom, periode3.tom), BigDecimal.valueOf(3))
    }

    @Test
    fun `perioder som er skilt med ukedag skal ikke slås sammen`() {
        val periode1 = Periode(uke1onsdag, uke1onsdag)
        val periode2 = Periode(uke1fredag, uke2mandag)
        val input: SortedMap<Periode, BigDecimal> = sortedMapOf(periode1 to BigDecimal.ONE,
                                                                periode2 to BigDecimal.ONE)
        val resultat: List<LogiskPeriode> = LogiskPeriodeTjeneste.utledLogiskPeriode(input)
        Assertions.assertThat(resultat).hasSize(2)
        assertSamme(resultat[0], periode1, BigDecimal.ONE)
        assertSamme(resultat[1], periode2, BigDecimal.ONE)
    }

    @Test
    fun `perioder som er skilt med helg skal slås sammen`() {
        val periode1 = Periode(uke1onsdag, uke1fredag)
        val periode2 = Periode(uke2mandag, uke2mandag)
        val input: SortedMap<Periode, BigDecimal> = sortedMapOf(periode1 to BigDecimal.ONE,
                                                                periode2 to BigDecimal.ONE)
        val resultat: List<LogiskPeriode> = LogiskPeriodeTjeneste.utledLogiskPeriode(input)
        Assertions.assertThat(resultat).hasSize(1)
        assertSamme(resultat[0], Periode(uke1onsdag, uke2mandag), BigDecimal.valueOf(2))
    }

    @Test
    fun `perioder som er skilt med en lørdag skal slås sammen`() {
        val periode1 = Periode(uke1onsdag, uke1fredag)
        val periode2 = Periode(uke1søndag, uke2mandag)
        val input: SortedMap<Periode, BigDecimal> = sortedMapOf(periode1 to BigDecimal.ONE,
                                                                periode2 to BigDecimal.ONE)
        val resultat: List<LogiskPeriode> = LogiskPeriodeTjeneste.utledLogiskPeriode(input)
        Assertions.assertThat(resultat).hasSize(1)
        assertSamme(resultat[0], Periode(uke1onsdag, uke2mandag), BigDecimal.valueOf(2))
    }

    @Test
    fun `perioder som er skilt med en søndag skal slås sammen`() {
        val periode1 = Periode(uke1onsdag, uke1lørdag)
        val periode2 = Periode(uke2mandag, uke2mandag)
        val input: SortedMap<Periode, BigDecimal> = sortedMapOf(periode1 to BigDecimal.ONE,
                                                                periode2 to BigDecimal.ONE)
        val resultat: List<LogiskPeriode> = LogiskPeriodeTjeneste.utledLogiskPeriode(input)
        Assertions.assertThat(resultat).hasSize(1)
        assertSamme(resultat[0], Periode(uke1onsdag, uke2mandag), BigDecimal.valueOf(2))
    }

    companion object {

        fun assertSamme(ub: LogiskPeriode, periode: Periode?, verdi: BigDecimal?) {
            Assertions.assertThat(Periode(ub.fom, ub.tom)).isEqualTo(periode)
            Assertions.assertThat(ub.feilutbetaltBeløp).isEqualByComparingTo(verdi)
        }
    }
}