package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import java.time.LocalDate

internal interface Saksbehandlingsteg {
    val type: Behandlingssteg
    val behandlingsstatus: BehandlingsstatusModell get() = BehandlingsstatusModell.TIL_BEHANDLING

    fun meldingerTilSaksbehandler(): Set<MeldingTilSaksbehandler> = emptySet()

    fun erFullstendig(klokke: Klokke): Boolean

    fun erUnderkjent(): Boolean

    fun underkjennSteget()

    fun erKlar(klokke: Klokke): Boolean {
        return erFullstendig(klokke) && !erUnderkjent()
    }

    fun nullstill(
        kravgrunnlag: KravgrunnlagHendelse,
        eksternFagsakRevurdering: EksternFagsakRevurdering,
    )

    fun automatiskVurder(
        kravgrunnlag: KravgrunnlagHendelse,
        dagensDato: LocalDate,
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
                this.erUnderkjent() -> Behandlingsstegstatus.TILBAKEFØRT
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
