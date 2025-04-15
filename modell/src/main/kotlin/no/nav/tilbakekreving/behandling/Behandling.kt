package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegsinfoDto
import no.nav.tilbakekreving.api.v1.dto.BeregnetPeriodeDto
import no.nav.tilbakekreving.api.v1.dto.BeregnetPerioderDto
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.saksbehandling.Faktasteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg.Companion.behandlingsstegstatus
import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg
import no.nav.tilbakekreving.brev.BrevHistorikk
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
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
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
    private val ansvarligSaksbehandler: String,
    private val eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
    val kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
    var foreldelsesteg: Foreldelsesteg,
    val faktasteg: Faktasteg,
    val vilkårsvurderingsteg: Vilkårsvurderingsteg,
    val foreslåVedtakSteg: ForeslåVedtakSteg,
) : Historikk.HistorikkInnslag<UUID>, FrontendDto<BehandlingDto> {
    private fun behandlingsstatus() =
        listOf(
            faktasteg,
            foreldelsesteg,
            vilkårsvurderingsteg,
            foreslåVedtakSteg,
        ).firstOrNull { !it.erFullstending() }
            ?.behandlingsstatus
            ?: Behandlingsstatus.AVSLUTTET

    fun beregnSplittetPeriode(perioder: List<Datoperiode>): BeregnetPerioderDto =
        BeregnetPerioderDto(
            perioder.map {
                BeregnetPeriodeDto(
                    it,
                    kravgrunnlag.entry.totaltBeløpFor(it),
                )
            },
        )

    fun splittForeldetPerioder(perioder: List<Datoperiode>) {
        foreldelsesteg.splittPerioder(perioder)
        vilkårsvurderingsteg.splittPerioder(perioder)
    }

    fun splittVilkårsvurdertePerioder(perioder: List<Datoperiode>) {
        vilkårsvurderingsteg.splittPerioder(perioder)
    }

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
                        vilkårsvurderingsteg.behandlingsstegstatus(),
                    ),
                    BehandlingsstegsinfoDto(
                        Behandlingssteg.FORESLÅ_VEDTAK,
                        foreldelsesteg.behandlingsstegstatus(),
                    ),
                ),
            fagsystemsbehandlingId = eksternFagsakBehandling.entry.eksternId,
            // TODO
            eksternFagsakId = "TODO",
            behandlingsårsakstype = årsak,
            støtterManuelleBrevmottakere = true,
            harManuelleBrevmottakere = false,
            manuelleBrevmottakere = emptyList(),
            begrunnelseForTilbakekreving = eksternFagsakBehandling.entry.begrunnelseForTilbakekreving,
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
            ansvarligSaksbehandler: String,
            eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
            kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
            brevHistorikk: BrevHistorikk,
        ): Behandling {
            val foreldelsesteg = Foreldelsesteg.opprett(kravgrunnlag)
            val faktasteg = Faktasteg.opprett(eksternFagsakBehandling, kravgrunnlag, brevHistorikk, LocalDateTime.now(), Opprettelsesvalg.OPPRETT_BEHANDLING_MED_VARSEL)
            val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(kravgrunnlag, foreldelsesteg)
            return Behandling(
                internId = internId,
                eksternId = eksternId,
                behandlingstype = behandlingstype,
                opprettet = opprettet,
                sistEndret = sistEndret,
                enhet = enhet,
                årsak = årsak,
                ansvarligSaksbehandler = ansvarligSaksbehandler,
                eksternFagsakBehandling = eksternFagsakBehandling,
                kravgrunnlag = kravgrunnlag,
                foreldelsesteg = foreldelsesteg,
                faktasteg = faktasteg,
                vilkårsvurderingsteg = vilkårsvurderingsteg,
                foreslåVedtakSteg = ForeslåVedtakSteg(faktasteg, foreldelsesteg, vilkårsvurderingsteg, kravgrunnlag),
            )
        }
    }
}
