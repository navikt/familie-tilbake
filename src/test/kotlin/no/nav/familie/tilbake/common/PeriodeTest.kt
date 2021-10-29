package no.nav.familie.tilbake.common

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class PeriodeTest {

    @Test
    fun `snitt returnerer lik periode for like perioder`() {
        val periode1 = Periode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))
        val periode2 = Periode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))
        val snitt = periode1.snitt(periode2)

        snitt shouldBe periode1
    }

    @Test
    fun `snitt returnerer null for periode uten overlap`() {
        val periode1 = Periode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))
        val periode2 = Periode(YearMonth.of(2018, 1), YearMonth.of(2018, 12))
        val snitt = periode1.snitt(periode2)

        snitt shouldBe null
    }

    @Test
    fun `snitt returnerer lik periode uansett hvilken periode som ligger til grunn`() {
        val periode1 = Periode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))
        val periode2 = Periode(YearMonth.of(2019, 3), YearMonth.of(2019, 12))
        val snitt1til2 = periode1.snitt(periode2)
        val snitt2til1 = periode2.snitt(periode1)

        snitt1til2 shouldBe snitt2til1
        snitt1til2 shouldBe Periode(YearMonth.of(2019, 3), YearMonth.of(2019, 5))
    }

    @Test
    fun `omslutter returnerer true for periode med overlap`() {
        val periode1 = Periode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))
        val periode2 = Periode(YearMonth.of(2019, 1), YearMonth.of(2019, 1))

        periode1.omslutter(periode2).shouldBeTrue()
    }

}
