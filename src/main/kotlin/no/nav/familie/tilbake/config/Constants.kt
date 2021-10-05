package no.nav.familie.tilbake.config

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Period


object Constants {

    private val rettsgebyrForDato = listOf(Datobeløp(LocalDate.of(2021, 1, 1), 1199))

    private val grunnbeløpForDato = listOf(Datobeløp(LocalDate.of(2021, 5, 1), 106399))

    private val brukersSvarfrist: Period = Period.ofWeeks(3)

    fun brukersSvarfrist() = LocalDate.now().plus(brukersSvarfrist)

    fun saksbehandlersTidsfrist() = brukersSvarfrist().plusDays(1)

    const val kravgrunnlagXmlRootElement: String = "urn:detaljertKravgrunnlagMelding"

    const val statusmeldingXmlRootElement: String = "urn:endringKravOgVedtakstatus"

    val rettsgebyr = rettsgebyrForDato.filter { it.gyldigFra <= LocalDate.now() }.maxByOrNull { it.gyldigFra }!!.beløp

    val grunnbeløp = grunnbeløpForDato.filter { it.gyldigFra <= LocalDate.now() }.maxByOrNull { it.gyldigFra }!!.beløp

    private class Datobeløp(val gyldigFra: LocalDate, val beløp: Long)

    const val BRUKER_ID_VEDTAKSLØSNINGEN = "VL"

    val MAKS_FEILUTBETALTBELØP_PER_YTELSE =
            mapOf<Ytelsestype, BigDecimal>(Ytelsestype.BARNETRYGD to BigDecimal.valueOf(rettsgebyr).multiply(BigDecimal(1.5)),
                                           Ytelsestype.BARNETILSYN to BigDecimal.valueOf(rettsgebyr).multiply(BigDecimal(0.5)),
                                           Ytelsestype.OVERGANGSSTØNAD to BigDecimal.valueOf(rettsgebyr)
                                                   .multiply(BigDecimal(0.5)),
                                           Ytelsestype.SKOLEPENGER to BigDecimal.valueOf(rettsgebyr)
                                                   .multiply(BigDecimal(0.5)),
                                           Ytelsestype.KONTANTSTØTTE to BigDecimal.valueOf(rettsgebyr)
                                                   .multiply(BigDecimal(0.5)))

    const val AUTOMATISK_SAKSBEHANDLING_BEGUNNLESE = "Automatisk satt verdi"

}

