package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.tilstand.AvventerBrukerinfo
import no.nav.tilbakekreving.tilstand.AvventerFagsysteminfo
import no.nav.tilbakekreving.tilstand.AvventerKravgrunnlag
import no.nav.tilbakekreving.tilstand.IverksettVedtak
import no.nav.tilbakekreving.tilstand.SendVarselbrev
import no.nav.tilbakekreving.tilstand.TilBehandling
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class TilbakekrevingEntity(
    val id: String,
    val nåværendeTilstand: String,
    val eksternFagsak: EksternFagsakEntity,
    val behandlingHistorikk: BehandlingHistorikkEntity,
    val kravgrunnlagHistorikk: KravgrunnlagHistorikkEntity,
    val brevHistorikk: BrevHistorikkEntity,
    val opprettet: String,
    val opprettelsesvalg: String,
    var bruker: BrukerEntity? = null,
) {
    fun fraEntity(
        behovObservatør: BehovObservatør,
    ): Tilbakekreving {
        val tilbakekreving = Tilbakekreving(
            id = UUID.fromString(id),
            eksternFagsak = eksternFagsak.fraEntity(behovObservatør),
            behandlingHistorikk = behandlingHistorikk.fraEntity(eksternFagsak, kravgrunnlagHistorikk),
            kravgrunnlagHistorikk = kravgrunnlagHistorikk.fraEntity(),
            brevHistorikk = brevHistorikk.fraEntity(),
            opprettet = LocalDateTime.parse(opprettet),
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
