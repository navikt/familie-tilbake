package no.nav.tilbakekreving.api.v2.fagsystem

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.tilbakekreving.api.v2.PeriodeDto
import java.math.BigDecimal
import java.time.LocalDateTime

data class BehandlingEndretHendelse(
    override val eksternFagsakId: String,
    override val hendelseOpprettet: LocalDateTime,
    val eksternBehandlingId: String?,
    val sakOpprettet: LocalDateTime,
    val varselSendt: LocalDateTime?,
    val behandlingsstatus: ForenkletBehandlingsstatus,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING)
    val totaltFeilutbetaltBeløp: BigDecimal,
    val saksbehandlingURL: String,
    val fullstendigPeriode: PeriodeDto,
) : Kafkamelding {
    companion object {
        val METADATA = EventMetadata<BehandlingEndretHendelse>(
            hendelsestype = "behandling_endret",
            versjon = 1,
        )
    }
}
