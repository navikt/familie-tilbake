package no.nav.tilbakekreving.hendelse

import java.time.LocalDate
import java.util.UUID

data class VarselbrevSendtHendelse(
    val varselbrevId: UUID,
    val journalpostId: String?,
    val tekstFraSaksbehandler: String,
    val sendtTid: LocalDate,
    val fristForUttalelse: LocalDate,
)
