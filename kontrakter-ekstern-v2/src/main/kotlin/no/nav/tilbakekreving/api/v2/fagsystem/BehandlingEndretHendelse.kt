package no.nav.tilbakekreving.api.v2.fagsystem

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.tilbakekreving.api.v2.PeriodeDto
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingEndretHendelse(
    override val eksternFagsakId: String,
    override val hendelseOpprettet: LocalDateTime,
    val eksternBehandlingId: String?,
    val tilbakekreving: Tilbakekreving,
) : Kafkamelding {
    data class Tilbakekreving(
        val behandlingId: UUID,
        val sakOpprettet: LocalDateTime,
        val varselSendt: LocalDate?,
        val behandlingsstatus: ForenkletBehandlingsstatus,
        @field:JsonFormat(shape = JsonFormat.Shape.STRING)
        val totaltFeilutbetaltBeløp: BigDecimal,
        val saksbehandlingURL: String,
        val fullstendigPeriode: PeriodeDto,
    )

    companion object {
        val METADATA = EventMetadata<BehandlingEndretHendelse>(
            hendelsestype = "behandling_endret",
            versjon = 1,
        )
    }
}
