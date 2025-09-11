package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus

internal interface Saksbehandlingsteg<FrontendDtoType> : FrontendDto<FrontendDtoType> {
    val type: Behandlingssteg
    val behandlingsstatus: Behandlingsstatus get() = Behandlingsstatus.UTREDES

    fun erFullstendig(): Boolean

    fun nullstill()

    companion object {
        fun <T> Saksbehandlingsteg<T>?.behandlingsstegstatus(): Behandlingsstegstatus {
            return when {
                this == null -> Behandlingsstegstatus.VENTER
                this.erFullstendig() -> Behandlingsstegstatus.UTFØRT
                else -> Behandlingsstegstatus.KLAR
            }
        }

        fun Collection<Saksbehandlingsteg<*>>.klarTilVisning(): List<Saksbehandlingsteg<*>> {
            val klarTilBehandling = mutableListOf<Saksbehandlingsteg<*>>()
            for (steg in this) {
                klarTilBehandling.add(steg)
                if (!steg.erFullstendig()) return klarTilBehandling
            }
            return klarTilBehandling
        }
    }
}
