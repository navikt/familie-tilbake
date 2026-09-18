package no.nav.tilbakekreving

import java.time.LocalDate

object Rettsgebyr {
    private val rettsgebyrForDato =
        listOf(
            // Det var samme rettsgebyr fra 2006–2015
            Datobeløp(LocalDate.of(2006, 1, 1), 860),
            Datobeløp(LocalDate.of(2016, 1, 1), 1025),
            Datobeløp(LocalDate.of(2017, 1, 1), 1049),
            Datobeløp(LocalDate.of(2018, 1, 1), 1130),
            Datobeløp(LocalDate.of(2019, 1, 1), 1150),
            Datobeløp(LocalDate.of(2020, 1, 1), 1172),
            Datobeløp(LocalDate.of(2021, 1, 1), 1199),
            Datobeløp(LocalDate.of(2022, 1, 1), 1223),
            Datobeløp(LocalDate.of(2023, 1, 1), 1243),
            Datobeløp(LocalDate.of(2024, 1, 1), 1277),
            Datobeløp(LocalDate.of(2025, 1, 1), 1314),
            Datobeløp(LocalDate.of(2026, 1, 1), 1345),
        )

    fun rettsgebyrForÅr(år: Int) = rettsgebyrForDato.filter { it.innført.year <= år }.maxByOrNull { it.innført }?.beløp

    val rettsgebyr = rettsgebyrForDato.filter { it.innført <= LocalDate.now() }.maxByOrNull { it.innført }!!.beløp

    fun fireRettsgebyrForÅr(år: Int) = rettsgebyrForÅr(år)?.times(4)
        ?: throw IllegalStateException("Rettsgebyr for år $år er ikke definert")

    private class Datobeløp(
        val innført: LocalDate,
        val beløp: Long,
    )
}
