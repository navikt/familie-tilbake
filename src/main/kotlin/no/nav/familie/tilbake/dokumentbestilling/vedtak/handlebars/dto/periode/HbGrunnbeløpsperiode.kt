package no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode

import java.math.BigDecimal
import java.time.LocalDate

data class HbGrunnbeløpsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val seksGangerBeløp: BigDecimal
)