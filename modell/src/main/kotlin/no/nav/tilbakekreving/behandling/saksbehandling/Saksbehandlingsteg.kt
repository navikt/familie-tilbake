package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus

internal interface Saksbehandlingsteg {
    val type: Behandlingssteg
    val behandlingsstatus: Behandlingsstatus get() = Behandlingsstatus.UTREDES

    fun meldingerTilSaksbehandler(): Set<MeldingTilSaksbehandler> = emptySet()

    fun erFullstendig(): Boolean

    fun erUnderkjent(): Boolean

    fun underkjennSteget()

    fun erKlar(): Boolean {
        return erFullstendig() && !erUnderkjent()
    }

    fun nullstill(
        kravgrunnlag: KravgrunnlagHendelse,
        eksternFagsakRevurdering: EksternFagsakRevurdering,
    )

    companion object {
        fun Saksbehandlingsteg?.behandlingsstegstatus(
            alleSynligeSteg: List<Saksbehandlingsteg>,
        ): Behandlingsstegstatus {
            val tidligereStegErTilbakeført = alleSynligeSteg
                .takeWhile { it != this }
                .any { it.erUnderkjent() }
            return when {
                this == null || tidligereStegErTilbakeført -> Behandlingsstegstatus.VENTER
                this.erUnderkjent() -> Behandlingsstegstatus.TILBAKEFØRT
                this.erFullstendig() -> Behandlingsstegstatus.UTFØRT
                else -> Behandlingsstegstatus.KLAR
            }
        }

        fun Collection<Saksbehandlingsteg>.klarTilVisning(): List<Saksbehandlingsteg> {
            val sisteFerdigstilteSteg = this.indexOfLast { it.erKlar() }
            return take(sisteFerdigstilteSteg + 2)
        }
    }
}
