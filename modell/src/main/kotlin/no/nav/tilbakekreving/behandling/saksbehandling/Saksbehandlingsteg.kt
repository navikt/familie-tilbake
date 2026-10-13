package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.behandlingslogg.Behandlingslogg
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.frontend.models.ArsakTilTilbakeforingDto
import no.nav.tilbakekreving.kravgrunnlag.EndretKravgrunnlagObservatør
import java.util.UUID

enum class ÅrsakTilTilbakeføring(val frontendDto: ArsakTilTilbakeforingDto) {
    NyttKravgrunnlag(ArsakTilTilbakeforingDto.NyttKravgrunnlag),
    Underkjent(ArsakTilTilbakeforingDto.TilbakemeldingFraSaksbehandler),
}

internal interface Saksbehandlingsteg : EndretKravgrunnlagObservatør {
    val type: Behandlingssteg
    val behandlingsstatus: BehandlingsstatusModell get() = BehandlingsstatusModell.TIL_BEHANDLING

    fun meldingerTilSaksbehandler(): Set<MeldingTilSaksbehandler> = emptySet()

    fun erFullstendig(klokke: Klokke): Boolean

    fun erPåbegynt(): Boolean

    fun trengerNyVurdering(): ÅrsakTilTilbakeføring?

    fun underkjennSteget()

    fun erKlar(klokke: Klokke): Boolean {
        return erFullstendig(klokke) && trengerNyVurdering() == null
    }

    fun nullstill(
        kravgrunnlag: KravgrunnlagHendelse,
        eksternFagsakRevurdering: EksternFagsakRevurdering,
    )

    fun automatiskVurder(
        kravgrunnlag: KravgrunnlagHendelse,
        klokke: Klokke,
        behandlingslogg: Behandlingslogg,
        behandlingId: UUID,
    ) {}

    fun venter(klokke: Klokke): Venter? = null

    companion object {
        fun Saksbehandlingsteg?.behandlingsstegstatus(
            alleSynligeSteg: List<Saksbehandlingsteg>,
            klokke: Klokke,
        ): Behandlingsstegstatus {
            val tidligereStegManglerBehandling = alleSynligeSteg
                .takeWhile { it != this }
                .any { !it.erKlar(klokke) }
            return when {
                this == null -> Behandlingsstegstatus.VENTER
                this.trengerNyVurdering() != null -> Behandlingsstegstatus.TILBAKEFØRT
                tidligereStegManglerBehandling -> Behandlingsstegstatus.VENTER
                this.erFullstendig(klokke) -> Behandlingsstegstatus.UTFØRT
                else -> Behandlingsstegstatus.KLAR
            }
        }

        fun Collection<Saksbehandlingsteg>.klarTilVisning(klokke: Klokke): List<Saksbehandlingsteg> {
            val sisteFerdigstilteSteg = this.indexOfLast { it.erKlar(klokke) }
            return take(sisteFerdigstilteSteg + 2)
        }
    }
}
