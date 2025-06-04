package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.saksbehandler.Behandler

data class BehandlerEntity(
    val type: String,
    val ident: String,
) {
    fun fraEntity(): Behandler {
        val behandler = when {
            type.equals("Saksbehandler") -> Behandler.Saksbehandler(ident)
            type.equals("Vedtaksløsning") -> Behandler.Vedtaksløsning
            else -> throw IllegalArgumentException("Ugyldig type $type")
        }
        return behandler
    }
}
