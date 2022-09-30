package no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Holder på grunnbeløpsperiode koblet til perioden for [HbVedtaksbrevsperiode]
 * @param seksGangerBeløp er gangret med seks då vi skal vise inntektsbeløper ganger seks
 * @param erFørste brukes fordi @last @first ikke virker innenfor en switch/case
 * @param erSiste brukes fordi @last @first ikke virker innenfor en switch/case
 */
data class HbGrunnbeløpsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val seksGangerBeløp: BigDecimal,
    val erFørste: Boolean,
    val erSiste: Boolean
)
