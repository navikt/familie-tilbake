package no.nav.familie.tilbake.beregning

import no.nav.familie.kontrakter.felles.Månedsperiode
import java.math.BigDecimal

object Grunnbeløpsperioder {

    fun finnGrunnbeløpsperioderForPeriode(periode: Månedsperiode): List<Grunnbeløp> {
        require(periode.tom <= grunnbeløpsperioderMaksTom) {
            "Har ikke lagt inn grunnbeløpsperiode frem til ${periode.tom}"
        }
        val perioder = grunnbeløpsperioder.filter {
            it.periode.overlapper(periode)
        }
        require(perioder.isNotEmpty()) {
            "Forventer å finne treff for ${periode.fom} - ${periode.tom} i grunnbeløpsperioder"
        }
        return perioder
    }
}

data class Grunnbeløp(
    val periode: Månedsperiode,
    val grunnbeløp: BigDecimal,
    val perMnd: BigDecimal,
    val gjennomsnittPerÅr: BigDecimal? = null
)

// Kopiert inn fra https://github.com/navikt/g
private val grunnbeløpsperioder: List<Grunnbeløp> =
    listOf(
        Grunnbeløp(
            periode = Månedsperiode("2022-05" to "2023-04"), // Setter ikke MAX for å unngå at grunnbeløpet ikke er oppdatert for neste periode
            grunnbeløp = 111_477.toBigDecimal(),
            perMnd = 9_290.toBigDecimal(),
            gjennomsnittPerÅr = 109_784.toBigDecimal()
        ),
        Grunnbeløp(
            periode = Månedsperiode("2021-05" to "2022-04"),
            grunnbeløp = 106_399.toBigDecimal(),
            perMnd = 8_867.toBigDecimal(),
            gjennomsnittPerÅr = 104_716.toBigDecimal()
        ),
        Grunnbeløp(
            periode = Månedsperiode("2020-05" to "2021-04"),
            grunnbeløp = 101_351.toBigDecimal(),
            perMnd = 8_446.toBigDecimal(),
            gjennomsnittPerÅr = 100_853.toBigDecimal()
        ),
        Grunnbeløp(
            periode = Månedsperiode("2019-05" to "2020-04"),
            grunnbeløp = 99_858.toBigDecimal(),
            perMnd = 8_322.toBigDecimal(),
            gjennomsnittPerÅr = 98_866.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("2018-05" to "2019-04"),
            grunnbeløp = 96_883.toBigDecimal(),
            perMnd = 8_074.toBigDecimal(),
            gjennomsnittPerÅr = 95_800.toBigDecimal()
        ),
        Grunnbeløp(
            periode = Månedsperiode("2017-05" to "2018-04"),
            grunnbeløp = 93_634.toBigDecimal(),
            perMnd = 7_803.toBigDecimal(),
            gjennomsnittPerÅr = 93_281.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("2016-05" to "2017-04"),
            grunnbeløp = 92_576.toBigDecimal(),
            perMnd = 7_715.toBigDecimal(),
            gjennomsnittPerÅr = 91_740.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("2015-05" to "2016-04"),
            grunnbeløp = 90_068.toBigDecimal(),
            perMnd = 7_506.toBigDecimal(),
            gjennomsnittPerÅr = 89_502.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("2014-05" to "2015-04"),
            grunnbeløp = 88_370.toBigDecimal(),
            perMnd = 7_364.toBigDecimal(),
            gjennomsnittPerÅr = 87_328.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("2013-05" to "2014-04"),
            grunnbeløp = 85_245.toBigDecimal(),
            perMnd = 7_104.toBigDecimal(),
            gjennomsnittPerÅr = 84_204.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("2012-05" to "2013-04"),
            grunnbeløp = 82_122.toBigDecimal(),
            perMnd = 6_844.toBigDecimal(),
            gjennomsnittPerÅr = 81_153.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("2011-05" to "2012-04"),
            grunnbeløp = 79_216.toBigDecimal(),
            perMnd = 6_601.toBigDecimal(),
            gjennomsnittPerÅr = 78_024.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("2010-05" to "2011-04"),
            grunnbeløp = 75_641.toBigDecimal(),
            perMnd = 6_303.toBigDecimal(),
            gjennomsnittPerÅr = 74_721.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("2009-05" to "2010-04"),
            grunnbeløp = 72_881.toBigDecimal(),
            perMnd = 6_073.toBigDecimal(),
            gjennomsnittPerÅr = 72_006.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("2008-05" to "2009-04"),
            grunnbeløp = 70_256.toBigDecimal(),
            perMnd = 5_855.toBigDecimal(),
            gjennomsnittPerÅr = 69_108.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("2007-05" to "2008-04"),
            grunnbeløp = 66_812.toBigDecimal(),
            perMnd = 5_568.toBigDecimal(),
            gjennomsnittPerÅr = 65_505.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("2006-05" to "2007-04"),
            grunnbeløp = 62_892.toBigDecimal(),
            perMnd = 5_241.toBigDecimal(),
            gjennomsnittPerÅr = 62_161.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("2005-05" to "2006-04"),
            grunnbeløp = 60_699.toBigDecimal(),
            perMnd = 5_058.toBigDecimal(),
            gjennomsnittPerÅr = 60_059.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("2004-05" to "2005-04"),
            grunnbeløp = 58_778.toBigDecimal(),
            perMnd = 4_898.toBigDecimal(),
            gjennomsnittPerÅr = 58_139.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("2003-05" to "2004-04"),
            grunnbeløp = 56_861.toBigDecimal(),
            perMnd = 4_738.toBigDecimal(),
            gjennomsnittPerÅr = 55_964.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("2002-05" to "2003-04"),
            grunnbeløp = 54_170.toBigDecimal(),
            perMnd = 4_514.toBigDecimal(),
            gjennomsnittPerÅr = 53_233.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("2001-05" to "2002-04"),
            grunnbeløp = 51_360.toBigDecimal(),
            perMnd = 4_280.toBigDecimal(),
            gjennomsnittPerÅr = 50_603.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("2000-05" to "2001-04"),
            grunnbeløp = 49_090.toBigDecimal(),
            perMnd = 4_091.toBigDecimal(),
            gjennomsnittPerÅr = 48_377.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1999-05" to "2000-04"),
            grunnbeløp = 46_950.toBigDecimal(),
            perMnd = 3_913.toBigDecimal(),
            gjennomsnittPerÅr = 46_423.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1998-05" to "1999-04"),
            grunnbeløp = 45_370.toBigDecimal(),
            perMnd = 3_781.toBigDecimal(),
            gjennomsnittPerÅr = 44_413.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1997-05" to "1998-04"),
            grunnbeløp = 42_500.toBigDecimal(),
            perMnd = 3_542.toBigDecimal(),
            gjennomsnittPerÅr = 42_000.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1996-05" to "1997-04"),
            grunnbeløp = 41_000.toBigDecimal(),
            perMnd = 3_417.toBigDecimal(),
            gjennomsnittPerÅr = 40_410.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1995-05" to "1996-04"),
            grunnbeløp = 39_230.toBigDecimal(),
            perMnd = 3_269.toBigDecimal(),
            gjennomsnittPerÅr = 38_847.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1994-05" to "1995-04"),
            grunnbeløp = 38_080.toBigDecimal(),
            perMnd = 3_173.toBigDecimal(),
            gjennomsnittPerÅr = 37_820.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1993-05" to "1994-04"),
            grunnbeløp = 37_300.toBigDecimal(),
            perMnd = 3_108.toBigDecimal(),
            gjennomsnittPerÅr = 37_033.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1992-05" to "1993-04"),
            grunnbeløp = 36_500.toBigDecimal(),
            perMnd = 3_042.toBigDecimal(),
            gjennomsnittPerÅr = 36_167.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1991-05" to "1992-04"),
            grunnbeløp = 35_500.toBigDecimal(),
            perMnd = 2_958.toBigDecimal(),
            gjennomsnittPerÅr = 35_033.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1990-12" to "1991-04"),
            grunnbeløp = 34_100.toBigDecimal(),
            perMnd = 2_842.toBigDecimal(),
            gjennomsnittPerÅr = 33_575.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1990-05" to "1990-11"),
            grunnbeløp = 34_000.toBigDecimal(),
            perMnd = 2_833.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1989-04" to "1990-04"),
            grunnbeløp = 32_700.toBigDecimal(),
            perMnd = 2_725.toBigDecimal(),
            gjennomsnittPerÅr = 32_275.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1988-04" to "1989-03"),
            grunnbeløp = 31_000.toBigDecimal(),
            perMnd = 2_583.toBigDecimal(),
            gjennomsnittPerÅr = 30_850.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1988-01" to "1988-03"),
            grunnbeløp = 30_400.toBigDecimal(),
            perMnd = 2_533.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1987-05" to "1987-12"),
            grunnbeløp = 29_900.toBigDecimal(),
            perMnd = 2_492.toBigDecimal(),
            gjennomsnittPerÅr = 29_267.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1986-05" to "1987-04"),
            grunnbeløp = 28_000.toBigDecimal(),
            perMnd = 2_333.toBigDecimal(),
            gjennomsnittPerÅr = 27_433.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1986-01" to "1986-04"),
            grunnbeløp = 26_300.toBigDecimal(),
            perMnd = 2_192.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1985-05" to "1985-12"),
            grunnbeløp = 25_900.toBigDecimal(),
            perMnd = 2_158.toBigDecimal(),
            gjennomsnittPerÅr = 25_333.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1984-05" to "1985-04"),
            grunnbeløp = 24_200.toBigDecimal(),
            perMnd = 2_017.toBigDecimal(),
            gjennomsnittPerÅr = 23_667.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1983-05" to "1984-04"),
            grunnbeløp = 22_600.toBigDecimal(),
            perMnd = 1_883.toBigDecimal(),
            gjennomsnittPerÅr = 22_333.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1983-01" to "1983-04"),
            grunnbeløp = 21_800.toBigDecimal(),
            perMnd = 1_817.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1982-05" to "1982-12"),
            grunnbeløp = 21_200.toBigDecimal(),
            perMnd = 1_767.toBigDecimal(),
            gjennomsnittPerÅr = 20_667.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1981-10" to "1982-04"),
            grunnbeløp = 19_600.toBigDecimal(),
            perMnd = 1_633.toBigDecimal(),
            gjennomsnittPerÅr = 18_658.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1981-05" to "1981-09"),
            grunnbeløp = 19_100.toBigDecimal(),
            perMnd = 1_592.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1981-01" to "1981-04"),
            grunnbeløp = 17_400.toBigDecimal(),
            perMnd = 1_450.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1980-05" to "1980-12"),
            grunnbeløp = 16_900.toBigDecimal(),
            perMnd = 1_408.toBigDecimal(),
            gjennomsnittPerÅr = 16_633.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1980-01" to "1980-04"),
            grunnbeløp = 16_100.toBigDecimal(),
            perMnd = 1_342.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1979-01" to "1979-12"),
            grunnbeløp = 15_200.toBigDecimal(),
            perMnd = 1_267.toBigDecimal(),
            gjennomsnittPerÅr = 15_200.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1978-07" to "1978-12"),
            grunnbeløp = 14_700.toBigDecimal(),
            perMnd = 1_225.toBigDecimal(),
            gjennomsnittPerÅr = 14_550.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1977-12" to "1978-06"),
            grunnbeløp = 14_400.toBigDecimal(),
            perMnd = 1_200.toBigDecimal(),
            gjennomsnittPerÅr = 13_383.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1977-05" to "1977-11"),
            grunnbeløp = 13_400.toBigDecimal(),
            perMnd = 1_117.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1977-01" to "1977-04"),
            grunnbeløp = 13_100.toBigDecimal(),
            perMnd = 1_092.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1976-05" to "1976-12"),
            grunnbeløp = 12_100.toBigDecimal(),
            perMnd = 1_008.toBigDecimal(),
            gjennomsnittPerÅr = 12_000.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1976-01" to "1976-04"),
            grunnbeløp = 11_800.toBigDecimal(),
            perMnd = 983.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1975-05" to "1975-12"),
            grunnbeløp = 11_000.toBigDecimal(),
            perMnd = 917.toBigDecimal(),
            gjennomsnittPerÅr = 10_800.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1975-01" to "1975-04"),
            grunnbeløp = 10_400.toBigDecimal(),
            perMnd = 867.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1974-05" to "1974-12"),
            grunnbeløp = 9_700.toBigDecimal(),
            perMnd = 808.toBigDecimal(),
            gjennomsnittPerÅr = 9_533.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1974-01" to "1974-04"),
            grunnbeløp = 9_200.toBigDecimal(),
            perMnd = 767.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1973-01" to "1973-12"),
            grunnbeløp = 8_500.toBigDecimal(),
            perMnd = 708.toBigDecimal(),
            gjennomsnittPerÅr = 8_500.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1972-01" to "1972-12"),
            grunnbeløp = 7_900.toBigDecimal(),
            perMnd = 658.toBigDecimal(),
            gjennomsnittPerÅr = 7_900.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1971-05" to "1971-12"),
            grunnbeløp = 7_500.toBigDecimal(),
            perMnd = 625.toBigDecimal(),
            gjennomsnittPerÅr = 7_400.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1971-01" to "1971-04"),
            grunnbeløp = 7_200.toBigDecimal(),
            perMnd = 600.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1970-01" to "1970-12"),
            grunnbeløp = 6_800.toBigDecimal(),
            perMnd = 567.toBigDecimal(),
            gjennomsnittPerÅr = 6_800.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1969-01" to "1969-12"),
            grunnbeløp = 6_400.toBigDecimal(),
            perMnd = 533.toBigDecimal(),
            gjennomsnittPerÅr = 6_400.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1968-01" to "1968-12"),
            grunnbeløp = 5_900.toBigDecimal(),
            perMnd = 492.toBigDecimal(),
            gjennomsnittPerÅr = 5_900.toBigDecimal()

        ),
        Grunnbeløp(
            periode = Månedsperiode("1967-01" to "1967-12"),
            grunnbeløp = 5_400.toBigDecimal(),
            perMnd = 450.toBigDecimal(),
            gjennomsnittPerÅr = 5_400.toBigDecimal()
        )
    )

private val grunnbeløpsperioderMaksTom = grunnbeløpsperioder.maxOf { it.periode.tom }
