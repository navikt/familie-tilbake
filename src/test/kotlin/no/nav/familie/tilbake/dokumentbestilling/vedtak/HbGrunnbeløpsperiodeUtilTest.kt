package no.nav.familie.tilbake.dokumentbestilling.vedtak

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.dokumentbestilling.vedtak.HbGrunnbeløpsperiodeUtil.utledGrunnbeløpsperioder
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class HbGrunnbeløpsperiodeUtilTest {

    @Test
    internal fun `skal bruke fra og til fra perioden hvis de er innenfor perioden sine datoer `() {
        val result = utledGrunnbeløpsperioder(Månedsperiode(YearMonth.of(2021, 3), YearMonth.of(2021, 3)))

        result shouldHaveSize 1
        result[0].fom shouldBe LocalDate.of(2021, 3, 1)
        result[0].tom shouldBe  LocalDate.of(2021, 3, 31)
    }

    @Test
    internal fun `skal multiplisere grunnbeløpet med 6`() {
        val result = utledGrunnbeløpsperioder(Månedsperiode(YearMonth.of(2021, 3), YearMonth.of(2021, 3)))

        result shouldHaveSize 1
        result[0].seksGangerBeløp.toInt() shouldBe 101_351 * 6
    }

    @Test
    internal fun `skal bruke periode sin fom hvis den er større enn beløpsperiode sin startdato`() {
        val result = utledGrunnbeløpsperioder(Månedsperiode(YearMonth.of(2021, 3), YearMonth.of(2021, 6)))

        result shouldHaveSize 2
        result[0].fom shouldBe LocalDate.of(2021, 3, 1)
        result[0].tom shouldBe  LocalDate.of(2021, 4, 30)

        result[1].fom shouldBe LocalDate.of(2021, 5, 1)
        result[1].tom shouldBe  LocalDate.of(2021, 6, 30)
    }

    @Test
    internal fun `skal bruke periode sin fom hvis den er større enn beløpsperiode sin startdato 2`() {
        val result = utledGrunnbeløpsperioder(Månedsperiode(YearMonth.of(2021, 4), YearMonth.of(2021, 5)))

        result shouldHaveSize 2
        result[0].fom shouldBe LocalDate.of(2021, 4, 1)
        result[0].tom shouldBe  LocalDate.of(2021, 4, 30)

        result[1].fom shouldBe LocalDate.of(2021, 5, 1)
        result[1].tom shouldBe  LocalDate.of(2021, 5, 31)
    }

    @Test
    internal fun `periode og beløpsperiode er lik`() {
        val result = utledGrunnbeløpsperioder(Månedsperiode(YearMonth.of(2021, 5), YearMonth.of(2022, 4)))

        result shouldHaveSize 1
        result[0].fom shouldBe LocalDate.of(2021, 5, 1)
        result[0].tom shouldBe  LocalDate.of(2022, 4, 30)
    }

    @Test
    internal fun `skal bruke beløpsperiode når perioden overlapper `() {
        val result = utledGrunnbeløpsperioder(Månedsperiode(YearMonth.of(2021, 3), YearMonth.of(2022, 6)))

        result shouldHaveSize 3
        result[0].fom shouldBe LocalDate.of(2021, 3, 1)
        result[0].tom shouldBe  LocalDate.of(2021, 4, 30)

        result[1].fom shouldBe LocalDate.of(2021, 5, 1)
        result[1].tom shouldBe  LocalDate.of(2022, 4, 30)

        result[2].fom shouldBe LocalDate.of(2022, 5, 1)
        result[2].tom shouldBe  LocalDate.of(2022, 6, 30)
    }
}