package no.nav.familie.tilbake.faktaomfeilutbetaling

import no.nav.familie.tilbake.common.Periode
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth
import kotlin.test.assertEquals

internal class LogiskPeriodeUtilTest {

    private val januar = YearMonth.of(2021, 1)
    private val februar = YearMonth.of(2021, 2)
    private val mars = YearMonth.of(2021, 3)
    private val april = YearMonth.of(2021, 4)
    private val mai = YearMonth.of(2021, 5)

    @Test
    fun `utledLogiskPeriode skal returnere én logisk periode når perioder kan slås sammen`() {
        val periode1 = Periode(januar, februar)
        val periode2 = Periode(mars, mai)

        val resultat = LogiskPeriodeUtil.utledLogiskPeriode(mapOf(periode1 to BigDecimal.valueOf(100),
                                                                  periode2 to BigDecimal.valueOf(200)).toSortedMap())

        assertEquals(1, resultat.size)
        assertEquals(januar, resultat[0].fom)
        assertEquals(mai, resultat[0].tom)
        assertEquals(BigDecimal.valueOf(300), resultat[0].feilutbetaltBeløp)
    }

    @Test
    fun `utledLogiskPeriode skal returner flere logiske periode når perioder som er skilt med måned ikke kan slås sammen`() {
        val periode1 = Periode(januar, februar)
        val periode2 = Periode(april, mai)

        val resultat = LogiskPeriodeUtil.utledLogiskPeriode(mapOf(periode1 to BigDecimal.valueOf(100),
                                                                  periode2 to BigDecimal.valueOf(200)).toSortedMap())

        assertEquals(2, resultat.size)
        assertEquals(januar, resultat[0].fom)
        assertEquals(februar, resultat[0].tom)
        assertEquals(BigDecimal.valueOf(100), resultat[0].feilutbetaltBeløp)

        assertEquals(april, resultat[1].fom)
        assertEquals(mai, resultat[1].tom)
        assertEquals(BigDecimal.valueOf(200), resultat[1].feilutbetaltBeløp)
    }
}
