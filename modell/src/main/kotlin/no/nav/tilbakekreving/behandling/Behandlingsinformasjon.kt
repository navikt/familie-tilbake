package no.nav.tilbakekreving.behandling

import java.time.LocalDateTime
import java.util.UUID

data class Behandlingsinformasjon(
    val kravgrunnlagReferanse: String,
    val opprettetTid: LocalDateTime,
    val behandlingId: UUID,
)
