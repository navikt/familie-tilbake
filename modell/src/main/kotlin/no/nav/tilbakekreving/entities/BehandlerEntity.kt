package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.saksbehandler.Behandler

data class BehandlerEntity(
    val type: BehandlerType,
    val ident: String,
) {
    fun fraEntity(): Behandler {
        return when (type) {
            BehandlerType.SAKSBEHANDLER -> Behandler.Saksbehandler(ident)
            BehandlerType.VEDTAKSLØSNING -> Behandler.Vedtaksløsning
        }
    }
}

enum class BehandlerType {
    SAKSBEHANDLER,
    VEDTAKSLØSNING,
}
