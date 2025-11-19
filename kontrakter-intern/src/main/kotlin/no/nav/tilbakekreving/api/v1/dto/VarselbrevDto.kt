package no.nav.tilbakekreving.api.v1.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class VarselbrevDto(
    val varselbrevSendtTid: LocalDateTime?,
    val uttalelsesfrist: LocalDate?,
)
