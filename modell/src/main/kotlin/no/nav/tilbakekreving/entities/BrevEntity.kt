package no.nav.tilbakekreving.entities

import java.time.LocalDate
import java.util.UUID

data class BrevEntity(
    val brevType: String,
    val internId: UUID,
    val opprettetDato: LocalDate,
    val varsletBeløp: Long? = null,
)
