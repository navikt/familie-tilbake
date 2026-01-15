package no.nav.tilbakekreving.api.v1.dto

import java.time.LocalDate

data class VarselbrevDto(
    val varselbrevSendtTid: LocalDate?,
    val opprinneligFristForUttalelse: LocalDate?,
    val tekstFraSaksbehandler: String?,
)
