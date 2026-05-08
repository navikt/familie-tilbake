package no.nav.tilbakekreving.fagsystem.events

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class Tilbakekreving(
    val behandlingId: UUID,
    val sakOpprettet: OffsetDateTime,
    val varselSendt: LocalDate,
    val behandlingsstatus: Behandlingsstatus,
    val forrigeBehandlingsstatus: Behandlingsstatus?,
    val totalFeilutbetaltBeløp: String,
    val saksbehandlingURL: String,
    val fullstendigPeriode: Periode,
)
