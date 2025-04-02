package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegsinfoDto
import no.nav.tilbakekreving.behandling.saksbehandling.Faktasteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreslåvedtaksteg
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg.Companion.behandlingsstegstatus
import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderderingsteg
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.behandling.Saksbehandlingstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import java.time.LocalDateTime
import java.util.UUID

class Behandling(
    override val internId: UUID,
    private val eksternId: UUID,
    private val behandlingstype: Behandlingstype,
    private val opprettet: LocalDateTime,
    private val sistEndret: LocalDateTime,
    private val enhet: Enhet?,
    private val årsak: Behandlingsårsakstype,
    private val begrunnelseForTilbakekreving: String,
    private val ansvarligSaksbehandler: String,
    // TODO: Når vi kan endre i front-end API burde vi fjerne eksternFagsakId fra behandling så vi ikke trenger det her
    private val eksternFagsak: EksternFagsak,
    private val eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
    var foreldelsesteg: Foreldelsesteg,
    val faktasteg: Faktasteg,
    val vilkårsvurderderingsteg: Vilkårsvurderderingsteg,
    val foreslåvedtaksteg: Foreslåvedtaksteg,
) : Historikk.HistorikkInnslag<UUID>, FrontendDto<BehandlingDto> {
    private fun behandlingsstatus() =
        listOf(
            faktasteg,
            foreldelsesteg,
            vilkårsvurderderingsteg,
            foreslåvedtaksteg,
        ).firstOrNull { !it.erFullstending() }
            ?.behandlingsstatus
            ?: Behandlingsstatus.AVSLUTTET

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
                        Behandlingssteg.GRUNNLAG,
                        Behandlingsstegstatus.AUTOUTFØRT,
                    ),
                    BehandlingsstegsinfoDto(
                        Behandlingssteg.VARSEL,
                        Behandlingsstegstatus.AUTOUTFØRT,
                    ),
                    BehandlingsstegsinfoDto(
                        Behandlingssteg.FAKTA,
                        faktasteg.behandlingsstegstatus(),
                    ),
                    BehandlingsstegsinfoDto(
                        Behandlingssteg.FORELDELSE,
                        foreldelsesteg.behandlingsstegstatus(),
                    ),
                    BehandlingsstegsinfoDto(
                        Behandlingssteg.VILKÅRSVURDERING,
                        vilkårsvurderderingsteg.behandlingsstegstatus(),
                    ),
                    BehandlingsstegsinfoDto(
                        Behandlingssteg.FORESLÅ_VEDTAK,
                        foreldelsesteg.behandlingsstegstatus(),
                    ),
                ),
            fagsystemsbehandlingId = eksternFagsakBehandling.entry.eksternId,
            eksternFagsakId = eksternFagsak.eksternId,
            behandlingsårsakstype = årsak,
            støtterManuelleBrevmottakere = true,
            harManuelleBrevmottakere = false,
            manuelleBrevmottakere = emptyList(),
            begrunnelseForTilbakekreving = begrunnelseForTilbakekreving,
            saksbehandlingstype = Saksbehandlingstype.ORDINÆR,
        )
    }

    companion object {
        fun nyBehandling(
            internId: UUID,
            eksternId: UUID,
            behandlingstype: Behandlingstype,
            opprettet: LocalDateTime,
            sistEndret: LocalDateTime = opprettet,
            enhet: Enhet?,
            årsak: Behandlingsårsakstype,
            begrunnelseForTilbakekreving: String,
            ansvarligSaksbehandler: String,
            eksternFagsak: EksternFagsak,
            eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
            kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
        ): Behandling {
            return Behandling(
                internId = internId,
                eksternId = eksternId,
                behandlingstype = behandlingstype,
                opprettet = opprettet,
                sistEndret = sistEndret,
                enhet = enhet,
                årsak = årsak,
                begrunnelseForTilbakekreving = begrunnelseForTilbakekreving,
                ansvarligSaksbehandler = ansvarligSaksbehandler,
                eksternFagsak = eksternFagsak,
                eksternFagsakBehandling = eksternFagsakBehandling,
                foreldelsesteg =
                    Foreldelsesteg(
                        vurdertePerioder =
                            kravgrunnlag.entry.datoperioder().map {
                                Foreldelsesteg.Foreldelseperiode(
                                    id = UUID.randomUUID(),
                                    periode = it,
                                    vurdering = Foreldelsesteg.Foreldelseperiode.Vurdering.IkkeVurdert,
                                )
                            },
                        kravgrunnlag = kravgrunnlag,
                    ),
                faktasteg = Faktasteg(0, eksternFagsakBehandling),
                vilkårsvurderderingsteg = Vilkårsvurderderingsteg(),
                foreslåvedtaksteg = Foreslåvedtaksteg(),
            )
        }
    }
}
