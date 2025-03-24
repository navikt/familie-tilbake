package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegsinfoDto
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.behandling.Saksbehandlingstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Behandling(
    private val internId: UUID,
    private val eksternId: UUID,
    private val behandlingstype: Behandlingstype,
    private val opprettet: LocalDateTime,
    private val sistEndret: LocalDateTime = opprettet,
    private val enhet: Enhet?,
    private val fagsystembehandling: EksternFagsakBehandling,
    private val årsak: Behandlingsårsakstype,
    private val begrunnelseForTilbakekreving: String,
    private val ansvarligSaksbehandler: String,
    // TODO: Når vi kan endre i front-end API burde vi fjerne eksternFagsakId fra behandling så vi ikke trenger det her
    private val eksternFagsak: EksternFagsak,
) : FrontendDto<BehandlingDto> {
    private fun behandlingsstatus() = Behandlingsstatus.UTREDES

    override fun tilFrontendDto(): BehandlingDto {
        return BehandlingDto(
            eksternBrukId = eksternId,
            behandlingId = internId,
            erBehandlingHenlagt = false,
            type = behandlingstype,
            status = behandlingsstatus(),
            opprettetDato = opprettet.toLocalDate(),
            avsluttetDato = null,
            endretTidspunkt = sistEndret,
            vedtaksdato = null,
            enhetskode = enhet?.kode ?: "Ukjent",
            enhetsnavn = enhet?.navn ?: "Ukjent",
            resultatstype = null,
            ansvarligSaksbehandler = ansvarligSaksbehandler,
            ansvarligBeslutter = null,
            erBehandlingPåVent = false,
            kanHenleggeBehandling = false,
            kanRevurderingOpprettes = true,
            harVerge = false,
            kanEndres = true,
            kanSetteTilbakeTilFakta = true,
            varselSendt = false,
            behandlingsstegsinfo =
                listOf(
                    BehandlingsstegsinfoDto(
                        behandlingssteg = Behandlingssteg.VARSEL,
                        behandlingsstegstatus = Behandlingsstegstatus.VENTER,
                        venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                        tidsfrist = LocalDate.now().minusDays(1),
                    ),
                ),
            fagsystemsbehandlingId = fagsystembehandling.eksternId,
            eksternFagsakId = eksternFagsak.eksternId,
            behandlingsårsakstype = årsak,
            støtterManuelleBrevmottakere = true,
            harManuelleBrevmottakere = false,
            manuelleBrevmottakere = emptyList(),
            begrunnelseForTilbakekreving = begrunnelseForTilbakekreving,
            saksbehandlingstype = Saksbehandlingstype.ORDINÆR,
        )
    }
}
