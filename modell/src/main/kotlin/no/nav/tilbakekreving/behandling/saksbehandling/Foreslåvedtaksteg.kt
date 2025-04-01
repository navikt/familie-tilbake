package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg

class Foreslåvedtaksteg : Saksbehandlingsteg<Any> {
    override val type = Behandlingssteg.FORESLÅ_VEDTAK

    override fun erFullstending(): Boolean = false

    override fun tilFrontendDto(): Any {
        return Unit
    }
}
