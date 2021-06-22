package no.nav.familie.tilbake.config

import java.time.LocalDate
import java.time.Period


object Constants {

    private val rettsgebyrForDato = listOf(Datobeløp(LocalDate.of(2021, 1, 1), 1199))

    private val grunnbeløpForDato = listOf(Datobeløp(LocalDate.of(2020, 5, 1), 101351))

    val brukersSvarfrist: Period = Period.ofWeeks(3)

    val kravgrunnlagXmlRootElement: String = "urn:detaljertKravgrunnlagMelding"

    val statusmeldingXmlRootElement: String = "urn:endringKravOgVedtakstatus"

    val rettsgebyr = rettsgebyrForDato.filter { it.gyldigFra <= LocalDate.now() }.maxByOrNull { it.gyldigFra }!!.beløp

    val grunnbeløp = grunnbeløpForDato.filter { it.gyldigFra <= LocalDate.now() }.maxByOrNull { it.gyldigFra }!!.beløp

    private class Datobeløp(val gyldigFra: LocalDate, val beløp: Long)
}

