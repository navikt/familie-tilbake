package no.nav.tilbakekreving

import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

fun Int.januar(år: Int = 2018): LocalDate = LocalDate.of(år, 1, this)

fun Int.februar(år: Int = 2018): LocalDate = LocalDate.of(år, 2, this)

fun Int.mars(år: Int = 2018): LocalDate = LocalDate.of(år, 3, this)

fun Int.april(år: Int = 2018): LocalDate = LocalDate.of(år, 4, this)

fun Int.mai(år: Int = 2018): LocalDate = LocalDate.of(år, 5, this)

fun Int.juni(år: Int = 2018): LocalDate = LocalDate.of(år, 6, this)

fun Int.juli(år: Int = 2018): LocalDate = LocalDate.of(år, 7, this)

fun Int.august(år: Int = 2018): LocalDate = LocalDate.of(år, 8, this)

fun Int.september(år: Int = 2018): LocalDate = LocalDate.of(år, 9, this)

fun Int.oktober(år: Int = 2018): LocalDate = LocalDate.of(år, 10, this)

fun Int.november(år: Int = 2018): LocalDate = LocalDate.of(år, 11, this)

fun Int.desember(år: Int = 2018): LocalDate = LocalDate.of(år, 12, this)

fun januar(år: Int = 2018) = YearMonth.of(år, Month.JANUARY)

fun februar(år: Int = 2018) = YearMonth.of(år, Month.FEBRUARY)

fun mars(år: Int = 2018) = YearMonth.of(år, Month.MARCH)

fun april(år: Int = 2018) = YearMonth.of(år, Month.APRIL)

fun mai(år: Int = 2018) = YearMonth.of(år, Month.MAY)

fun juni(år: Int = 2018) = YearMonth.of(år, Month.JUNE)

fun juli(år: Int = 2018) = YearMonth.of(år, Month.JULY)

fun august(år: Int = 2018) = YearMonth.of(år, Month.AUGUST)

fun september(år: Int = 2018) = YearMonth.of(år, Month.SEPTEMBER)

fun oktober(år: Int = 2018) = YearMonth.of(år, Month.OCTOBER)

fun november(år: Int = 2018) = YearMonth.of(år, Month.NOVEMBER)

fun desember(år: Int = 2018) = YearMonth.of(år, Month.DECEMBER)
