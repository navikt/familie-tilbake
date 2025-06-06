package no.nav.tilbakekreving.saksbehandler

import no.nav.tilbakekreving.entities.BehandlerEntity

sealed interface Behandler {
    val ident: String

    fun tilEntity(): BehandlerEntity

    class Saksbehandler(override val ident: String) : Behandler {
        override fun equals(other: Any?): Boolean {
            return ident == (other as? Saksbehandler)?.ident
        }

        override fun tilEntity(): BehandlerEntity {
            return BehandlerEntity("Saksbehandler", ident)
        }

        override fun hashCode(): Int = ident.hashCode()
    }

    data object Vedtaksløsning : Behandler {
        override val ident: String = "VL"

        override fun tilEntity(): BehandlerEntity {
            return BehandlerEntity("Vedtaksløsning", ident)
        }
    }
}
