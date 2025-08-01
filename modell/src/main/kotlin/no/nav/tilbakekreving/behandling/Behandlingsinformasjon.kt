package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import java.time.LocalDateTime
import java.util.UUID

data class Behandlingsinformasjon(
    val kravgrunnlagReferanse: String,
    val opprettetTid: LocalDateTime,
    val behandlingId: UUID,
    val enhet: Enhet?,
    val behandlingstype: Behandlingstype,
)
