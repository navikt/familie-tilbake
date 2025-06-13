package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.BehandlingHistorikk
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import no.nav.tilbakekreving.tilstand.AvventerBrukerinfo
import no.nav.tilbakekreving.tilstand.AvventerFagsysteminfo
import no.nav.tilbakekreving.tilstand.AvventerKravgrunnlag
import no.nav.tilbakekreving.tilstand.IverksettVedtak
import no.nav.tilbakekreving.tilstand.SendVarselbrev
import no.nav.tilbakekreving.tilstand.TilBehandling
import java.time.LocalDateTime
import java.util.UUID

data class TilbakekrevingEntity(
    val id: UUID,
    val fagsystemId: String,
    val nåværendeTilstand: TilbakekrevingTilstand,
    val eksternFagsak: EksternFagsakEntity,
    val behandlingHistorikkEntities: List<BehandlingEntity>,
    val kravgrunnlagHistorikkEntities: List<KravgrunnlagHendelseEntity>,
    val brevHistorikkEntities: List<BrevEntity>,
    val opprettet: LocalDateTime,
    val opprettelsesvalg: Opprettelsesvalg,
    var bruker: BrukerEntity? = null,
) {
    fun fraEntity(
        behovObservatør: BehovObservatør,
    ): Tilbakekreving {
        val kravgrunnlagHistorikk = KravgrunnlagHistorikk(
            historikk = kravgrunnlagHistorikkEntities.map { it.fraEntity() }.toMutableList(),
        )

        val eksternFagsakBehandlingHistorikk = EksternFagsakBehandlingHistorikk(eksternFagsak.behandlinger.map { it.fraEntity() }.toMutableList())

        val brevHistorikk = BrevHistorikk(
            historikk = brevHistorikkEntities.map { it.fraEntity() }.toMutableList(),
        )

        val behandlingHistorikk = BehandlingHistorikk(
            historikk = behandlingHistorikkEntities.map {
                Behandling.fraEntity(
                    behandlingEntity = it,
                    eksternFagsakBehandlingHistorikk = eksternFagsakBehandlingHistorikk,
                    kravgrunnlagHistorikk = kravgrunnlagHistorikk,
                    brevHistorikk = brevHistorikk,
                )
            }.toMutableList(),
        )

        val tilbakekreving = Tilbakekreving(
            id = id,
            fagsystemId = fagsystemId,
            eksternFagsak = eksternFagsak.fraEntity(behovObservatør),
            behandlingHistorikk = behandlingHistorikk,
            kravgrunnlagHistorikk = kravgrunnlagHistorikk,
            brevHistorikk = brevHistorikk,
            opprettet = opprettet,
            opprettelsesvalg = opprettelsesvalg,
            bruker = bruker?.fraEntity(),
            behovObservatør = behovObservatør,
        )
        when (nåværendeTilstand) {
            TilbakekrevingTilstand.AVVENTER_KRAVGRUNNLAG -> tilbakekreving.byttTilstand(AvventerKravgrunnlag)
            TilbakekrevingTilstand.AVVENTER_FAGSYSTEMINFO -> tilbakekreving.byttTilstand(AvventerFagsysteminfo)
            TilbakekrevingTilstand.AVVENTER_BRUKERINFO -> tilbakekreving.byttTilstand(AvventerBrukerinfo)
            TilbakekrevingTilstand.SEND_VARSELBREV -> tilbakekreving.byttTilstand(SendVarselbrev)
            TilbakekrevingTilstand.IVERKSETT_VEDTAK -> tilbakekreving.byttTilstand(IverksettVedtak)
            TilbakekrevingTilstand.TIL_BEHANDLING -> tilbakekreving.byttTilstand(TilBehandling)
            else -> throw IllegalArgumentException("Ugyldig tilstandsnavn $nåværendeTilstand")
        }
        return tilbakekreving
    }
}
