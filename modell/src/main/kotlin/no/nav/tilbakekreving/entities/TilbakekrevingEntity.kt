package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.BehandlingHistorikk
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.bigquery.BigQueryService
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.endring.EndringObservatør
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import no.nav.tilbakekreving.tilstand.Avsluttet
import no.nav.tilbakekreving.tilstand.AvventerBrukerinfo
import no.nav.tilbakekreving.tilstand.AvventerFagsysteminfo
import no.nav.tilbakekreving.tilstand.AvventerKravgrunnlag
import no.nav.tilbakekreving.tilstand.IverksettVedtak
import no.nav.tilbakekreving.tilstand.SendVarselbrev
import no.nav.tilbakekreving.tilstand.Start
import no.nav.tilbakekreving.tilstand.TilBehandling
import java.time.LocalDateTime

data class TilbakekrevingEntity(
    val id: String,
    val nåværendeTilstand: TilbakekrevingTilstand,
    val eksternFagsak: EksternFagsakEntity,
    val behandlingHistorikkEntities: List<BehandlingEntity>,
    val kravgrunnlagHistorikkEntities: List<KravgrunnlagHendelseEntity>,
    val brevHistorikkEntities: List<BrevEntity>,
    val opprettet: LocalDateTime,
    val nestePåminnelse: LocalDateTime?,
    val opprettelsesvalg: Opprettelsesvalg,
    val bruker: BrukerEntity?,
) {
    fun fraEntity(
        behovObservatør: BehovObservatør,
        bigQueryService: BigQueryService,
        endringObservatør: EndringObservatør,
    ): Tilbakekreving {
        val kravgrunnlagHistorikk = KravgrunnlagHistorikk(
            historikk = kravgrunnlagHistorikkEntities.map { it.fraEntity() }.toMutableList(),
        )

        val eksternFagsakBehandlingHistorikk = EksternFagsakBehandlingHistorikk(eksternFagsak.behandlinger.map { it.fraEntity() }.toMutableList())

        val brevHistorikk = BrevHistorikk(
            historikk = brevHistorikkEntities.map { it.fraEntity(kravgrunnlagHistorikk) }.toMutableList(),
        )

        val behandlingHistorikk = BehandlingHistorikk(
            historikk = behandlingHistorikkEntities.map {
                it.fraEntity(
                    eksternFagsakBehandlingHistorikk = eksternFagsakBehandlingHistorikk,
                    kravgrunnlagHistorikk = kravgrunnlagHistorikk,
                    brevHistorikk = brevHistorikk,
                )
            }.toMutableList(),
        )

        val tilbakekreving = Tilbakekreving(
            id = id,
            eksternFagsak = eksternFagsak.fraEntity(behovObservatør),
            behandlingHistorikk = behandlingHistorikk,
            kravgrunnlagHistorikk = kravgrunnlagHistorikk,
            brevHistorikk = brevHistorikk,
            opprettet = opprettet,
            opprettelsesvalg = opprettelsesvalg,
            nestePåminnelse = nestePåminnelse,
            bruker = bruker?.fraEntity(),
            behovObservatør = behovObservatør,
            tilstand = when (nåværendeTilstand) {
                TilbakekrevingTilstand.START -> Start
                TilbakekrevingTilstand.AVVENTER_KRAVGRUNNLAG -> AvventerKravgrunnlag
                TilbakekrevingTilstand.AVVENTER_FAGSYSTEMINFO -> AvventerFagsysteminfo
                TilbakekrevingTilstand.AVVENTER_BRUKERINFO -> AvventerBrukerinfo
                TilbakekrevingTilstand.SEND_VARSELBREV -> SendVarselbrev
                TilbakekrevingTilstand.IVERKSETT_VEDTAK -> IverksettVedtak
                TilbakekrevingTilstand.TIL_BEHANDLING -> TilBehandling
                TilbakekrevingTilstand.AVSLUTTET -> Avsluttet
            },
            bigQueryService = bigQueryService,
            endringObservatør = endringObservatør,
        )

        return tilbakekreving
    }
}
