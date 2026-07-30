package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.BehandlingHistorikk
import no.nav.tilbakekreving.brev.Brev
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import no.nav.tilbakekreving.tilstand.Avsluttet
import no.nav.tilbakekreving.tilstand.AvventerBrukerinfo
import no.nav.tilbakekreving.tilstand.AvventerFagsysteminfo
import no.nav.tilbakekreving.tilstand.AvventerKravgrunnlag
import no.nav.tilbakekreving.tilstand.DistribuerVarselbrev
import no.nav.tilbakekreving.tilstand.DistribuerVedtak
import no.nav.tilbakekreving.tilstand.IverksettVedtak
import no.nav.tilbakekreving.tilstand.JournalførVedtak
import no.nav.tilbakekreving.tilstand.SendVarselbrev
import no.nav.tilbakekreving.tilstand.Start
import no.nav.tilbakekreving.tilstand.TilBehandling
import java.time.LocalDateTime
import java.util.UUID

data class TilbakekrevingEntity(
    val id: String,
    val nåværendeTilstand: TilbakekrevingTilstand,
    val eksternFagsak: EksternFagsakEntity,
    val behandlingHistorikkEntities: HistorikkEntity<UUID, BehandlingEntity, Behandling>,
    val kravgrunnlagHistorikkEntities: HistorikkEntity<UUID, KravgrunnlagHendelseEntity, KravgrunnlagHendelse>,
    val brevHistorikkEntities: HistorikkEntity<UUID, BrevEntity, Brev>,
    val opprettet: LocalDateTime,
    val nestePåminnelse: LocalDateTime?,
    val opprettelsesvalg: Opprettelsesvalg,
    val bruker: BrukerEntity?,
) {
    fun fraEntity(): Tilbakekreving {
        val kravgrunnlagHistorikk = KravgrunnlagHistorikk(
            historikk = kravgrunnlagHistorikkEntities.fraEntity { it.fraEntity() }.toMutableList(),
        )

        val eksternFagsakBehandlingHistorikk = EksternFagsakBehandlingHistorikk(eksternFagsak.behandlinger.fraEntity { it.fraEntity() }.toMutableList())

        val brevHistorikk = BrevHistorikk(
            historikk = brevHistorikkEntities.fraEntity { it.fraEntity(kravgrunnlagHistorikk) }.toMutableList(),
        )

        val behandlingHistorikk = BehandlingHistorikk(
            historikk = behandlingHistorikkEntities.fraEntity {
                it.fraEntity(
                    eksternFagsakBehandlingHistorikk = eksternFagsakBehandlingHistorikk,
                    kravgrunnlagHistorikk = kravgrunnlagHistorikk,
                    brevHistorikk = brevHistorikk,
                )
            }.toMutableList(),
        )

        val tilbakekreving = Tilbakekreving(
            id = id,
            eksternFagsak = eksternFagsak.fraEntity(),
            behandlingHistorikk = behandlingHistorikk,
            kravgrunnlagHistorikk = kravgrunnlagHistorikk,
            brevHistorikk = brevHistorikk,
            opprettet = opprettet,
            opprettelsesvalg = opprettelsesvalg,
            nestePåminnelse = nestePåminnelse,
            bruker = bruker?.fraEntity(),
            tilstand = when (nåværendeTilstand) {
                TilbakekrevingTilstand.START -> Start
                TilbakekrevingTilstand.AVVENTER_KRAVGRUNNLAG -> AvventerKravgrunnlag
                TilbakekrevingTilstand.AVVENTER_FAGSYSTEMINFO -> AvventerFagsysteminfo
                TilbakekrevingTilstand.AVVENTER_BRUKERINFO -> AvventerBrukerinfo
                TilbakekrevingTilstand.SEND_VARSELBREV -> SendVarselbrev
                TilbakekrevingTilstand.DISTRIUBER_VARSELBREV -> DistribuerVarselbrev
                TilbakekrevingTilstand.IVERKSETT_VEDTAK -> IverksettVedtak
                TilbakekrevingTilstand.TIL_BEHANDLING -> TilBehandling
                TilbakekrevingTilstand.JOURNALFØR_VEDTAK -> JournalførVedtak
                TilbakekrevingTilstand.DISTRIUBER_VEDTAK -> DistribuerVedtak
                TilbakekrevingTilstand.AVSLUTTET -> Avsluttet
            },
        )

        return tilbakekreving
    }
}
