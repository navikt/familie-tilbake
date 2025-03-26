package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus

internal interface Saksbehandlingsteg<FrontendDtoType> : FrontendDto<FrontendDtoType> {
    val type: Behandlingssteg
    val behandlingsstatus: Behandlingsstatus get() = Behandlingsstatus.UTREDES

    fun erFullstending(): Boolean

    companion object {
        fun <T> Saksbehandlingsteg<T>?.behandlingsstegstatus(): Behandlingsstegstatus {
            return when {
                this == null -> Behandlingsstegstatus.VENTER
                this.erFullstending() -> Behandlingsstegstatus.UTFØRT
                else -> Behandlingsstegstatus.KLAR
            }
        }
    }
}
