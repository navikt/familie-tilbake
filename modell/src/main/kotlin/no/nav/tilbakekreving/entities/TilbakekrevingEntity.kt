package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.BehandlingHistorikk
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
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
    val nåværendeTilstand: String,
    val eksternFagsak: EksternFagsakEntity,
    val behandlingHistorikkEntities: List<BehandlingEntity>,
    val kravgrunnlagHistorikkEntities: List<KravgrunnlagHendelseEntity>,
    val brevHistorikkEntities: List<BrevEntity>,
    val opprettet: LocalDateTime,
    val opprettelsesvalg: String,
    var bruker: BrukerEntity? = null,
) {
    fun fraEntity(
        behovObservatør: BehovObservatør,
    ): Tilbakekreving {
        val kravgrunnlagHistorikk = KravgrunnlagHistorikk(
            historikk = kravgrunnlagHistorikkEntities.map { it.fraEntity() }.toMutableList(),
        )

        val eksternFagsakBehandling = EksternFagsakBehandlingHistorikk(eksternFagsak.behandlinger.map { it.fraEntity() }.toMutableList()).nåværende()

        val behandlingHistorikk = BehandlingHistorikk(
            historikk = behandlingHistorikkEntities.map {
                Behandling.fraEntity(
                    behandlingEntity = it,
                    eksternFagsak = eksternFagsakBehandling,
                    kravgrunnlagHistorikk = kravgrunnlagHistorikk,
                )
            }.toMutableList(),
        )

        val brevHistorikk = BrevHistorikk(
            historikk = brevHistorikkEntities.map { it.fraEntity() }.toMutableList(),
        )

        val tilbakekreving = Tilbakekreving(
            id = id,
            eksternFagsak = eksternFagsak.fraEntity(behovObservatør),
            behandlingHistorikk = behandlingHistorikk,
            kravgrunnlagHistorikk = kravgrunnlagHistorikk,
            brevHistorikk = brevHistorikk,
            opprettet = opprettet,
            opprettelsesvalg = Opprettelsesvalg.valueOf(opprettelsesvalg),
            bruker = bruker?.fraEntity(),
            behovObservatør = behovObservatør,
        )
        when {
            nåværendeTilstand.equals("AvventerKravgrunnlag") -> tilbakekreving.byttTilstand(AvventerKravgrunnlag)
            nåværendeTilstand.equals("AvventerFagsysteminfo") -> tilbakekreving.byttTilstand(AvventerFagsysteminfo)
            nåværendeTilstand.equals("AvventerBrukerinfo") -> tilbakekreving.byttTilstand(AvventerBrukerinfo)
            nåværendeTilstand.equals("SendVarselbrev") -> tilbakekreving.byttTilstand(SendVarselbrev)
            nåværendeTilstand.equals("IverksettVedtak") -> tilbakekreving.byttTilstand(IverksettVedtak)
            nåværendeTilstand.equals("TilBehandling") -> tilbakekreving.byttTilstand(TilBehandling)
            else -> throw IllegalArgumentException("Ugyldig tilstandsnavn $nåværendeTilstand")
        }
        return tilbakekreving
    }
}
