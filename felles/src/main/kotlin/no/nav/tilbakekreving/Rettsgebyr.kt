package no.nav.tilbakekreving

import java.time.LocalDate

object Rettsgebyr {
    private val rettsgebyrForDato =
        listOf(
            Datobeløp(LocalDate.of(2021, 1, 1), 1199),
            Datobeløp(LocalDate.of(2022, 1, 1), 1223),
            Datobeløp(LocalDate.of(2023, 1, 1), 1243),
            Datobeløp(LocalDate.of(2024, 1, 1), 1277),
            Datobeløp(LocalDate.of(2025, 1, 1), 1314),
        )

    fun rettsgebyrForÅr(år: Int) = rettsgebyrForDato.filter { it.gyldigFra.year <= år }.maxByOrNull { it.gyldigFra }?.beløp

    val rettsgebyr = rettsgebyrForDato.filter { it.gyldigFra <= LocalDate.now() }.maxByOrNull { it.gyldigFra }!!.beløp

    private class Datobeløp(
        val gyldigFra: LocalDate,
        val beløp: Long,
    )
}
