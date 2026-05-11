package no.nav.tilbakekreving.fagsystem.events

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class TilbakekrevingEventDto(
    val behandlingId: UUID,
    val sakOpprettet: OffsetDateTime,
    val venter: VenterEventDto?,
    val varselSendt: LocalDate?,
    val behandlingsstatus: BehandlingsstatusEventDto,
    val forrigeBehandlingsstatus: BehandlingsstatusEventDto?,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING)
    val totaltFeilutbetaltBeløp: BigDecimal,
    val saksbehandlingURL: String,
    val fullstendigPeriode: PeriodeEventDto,
)
