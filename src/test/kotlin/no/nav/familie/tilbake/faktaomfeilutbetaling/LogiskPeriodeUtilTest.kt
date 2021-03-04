package no.nav.familie.tilbake.faktaomfeilutbetaling

import no.nav.familie.tilbake.common.Periode
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

internal class LogiskPeriodeUtilTest {

    private val uke1Fredag = LocalDate.of(2021, 1, 1)
    private val uke1Søndag = uke1Fredag.plusDays(2)
    private val uke2Mandag = uke1Fredag.plusDays(3)
    private val uke2Tirsdag = uke2Mandag.plusDays(1)
    private val uke2Onsdag = uke2Mandag.plusDays(2)
    private val uke3Mandag = uke2Mandag.plusDays(7)

    private val comparator = Comparator.comparing(Periode::fom).thenComparing(Periode::tom)

    @Test
    fun `utledLogiskPeriode skal hente logiske periode når perioder kan slå sammen`() {
        val periode1 = Periode(uke1Fredag, uke2Mandag)
        val periode2 = Periode(uke2Tirsdag, uke3Mandag)

        val resultat = LogiskPeriodeUtil.utledLogiskPeriode(mapOf(periode1 to BigDecimal.valueOf(100),
                                                                  periode2 to BigDecimal.valueOf(200)).toSortedMap(comparator))

        assertEquals(1, resultat.size)
        assertEquals(uke1Fredag, resultat[0].fom)
        assertEquals(uke3Mandag, resultat[0].tom)
        assertEquals(BigDecimal.valueOf(300), resultat[0].feilutbetaltBeløp)
    }

    @Test
    fun `utledLogiskPeriode skal hente logiske periode når perioder som er skilt med ukedag kan ikke slå sammen`() {
        val periode1 = Periode(uke1Fredag, uke2Mandag)
        val periode2 = Periode(uke2Onsdag, uke3Mandag)

        val resultat = LogiskPeriodeUtil.utledLogiskPeriode(mapOf(periode1 to BigDecimal.valueOf(100),
                                                                  periode2 to BigDecimal.valueOf(200)).toSortedMap(comparator))

        assertEquals(2, resultat.size)
        assertEquals(uke1Fredag, resultat[0].fom)
        assertEquals(uke2Mandag, resultat[0].tom)
        assertEquals(BigDecimal.valueOf(100), resultat[0].feilutbetaltBeløp)

        assertEquals(uke2Onsdag, resultat[1].fom)
        assertEquals(uke3Mandag, resultat[1].tom)
        assertEquals(BigDecimal.valueOf(200), resultat[1].feilutbetaltBeløp)
    }

    @Test
    fun `utledLogiskPeriode skal hente logiske periode når perioder som er skilt med helg kan slå sammen`() {
        val periode1 = Periode(uke1Fredag, uke1Fredag)
        val periode2 = Periode(uke2Mandag, uke3Mandag)

        val resultat = LogiskPeriodeUtil.utledLogiskPeriode(mapOf(periode1 to BigDecimal.valueOf(100),
                                                                  periode2 to BigDecimal.valueOf(200)).toSortedMap(comparator))

        assertEquals(1, resultat.size)
        assertEquals(uke1Fredag, resultat[0].fom)
        assertEquals(uke3Mandag, resultat[0].tom)
        assertEquals(BigDecimal.valueOf(300), resultat[0].feilutbetaltBeløp)
    }

    @Test
    fun `utledLogiskPeriode skal hente logiske periode når perioder som er skilt med lørdag kan slå sammen`() {
        val periode1 = Periode(uke1Fredag, uke1Fredag)
        val periode2 = Periode(uke1Søndag, uke3Mandag)

        val resultat = LogiskPeriodeUtil.utledLogiskPeriode(mapOf(periode1 to BigDecimal.valueOf(100),
                                                                  periode2 to BigDecimal.valueOf(200)).toSortedMap(comparator))

        assertEquals(1, resultat.size)
        assertEquals(uke1Fredag, resultat[0].fom)
        assertEquals(uke3Mandag, resultat[0].tom)
        assertEquals(BigDecimal.valueOf(300), resultat[0].feilutbetaltBeløp)
    }
}
